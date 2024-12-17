import { createContext, ReactNode, useCallback, useContext, useReducer, useState } from 'react';
import { DataCategory, DataCategoryTypeMap } from '@features/material-details/hooks/useMaterialDetails';
import { Site } from '@models/types/edc/site';
import { ModalMode } from '@models/types/data/modal-mode';
import { Delivery } from '@models/types/data/delivery';
import { DemandCategoryModal } from '@features/material-details/components/DemandCategoryModal';
import { PlannedProductionModal } from '@features/material-details/components/PlannedProductionModal';
import { DeliveryInformationModal } from '@features/material-details/components/DeliveryInformationModal';
import { DEMAND_CATEGORY } from '@models/constants/demand-category';
import { Demand } from '@models/types/data/demand';
import { Production } from '@models/types/data/production';
import { Material } from '@models/types/data/stock';

type DataModalContext = {
    openDialog: <TCategory extends DataCategory>(
        category: TCategory,
        data: Partial<DataCategoryTypeMap[TCategory]>,
        mode: ModalMode,
        direction?: 'incoming' | 'outgoing',
        site?: Site | null
    ) => void;
    addOnSaveListener: (callback: (category: DataCategory) => void) => void;
};

const dataModalContext = createContext<DataModalContext | null>(null);

type DataModalProviderProps = {
    children: ReactNode;
    material?: Material;
};

export const DataModalProvider = ({ children, material }: DataModalProviderProps) => {
    const [state, dispatch] = useReducer(reducer, initialState);
    const [onSaveListeners, setOnSaveListeners] = useState<((category: DataCategory) => void)[]>([]);
    const materialNumber = material?.ownMaterialNumber ?? '';

    const addOnSaveListener = useCallback((callback: (category: DataCategory) => void) => {
        setOnSaveListeners((prev) => [...prev, callback]);
    }, []);

    const onSave = useCallback((category: DataCategory) => {
        onSaveListeners.forEach((callback) => callback(category));
    }, [onSaveListeners]);

    const openDemandDialog = useCallback(
        (d: Partial<Demand>, mode: ModalMode) => {
            d.measurementUnit ??= 'unit:piece';
            d.demandCategoryCode ??= DEMAND_CATEGORY[0]?.key;
            d.ownMaterialNumber = materialNumber;
            dispatch({ type: 'demand', payload: d });
            dispatch({ type: 'demandDialogOptions', payload: { open: true, mode } });
        },
        [materialNumber]
    );

    const openProductionDialog = useCallback(
        (p: Partial<Production>, mode: ModalMode) => {
            p.material ??= {
                materialFlag: true,
                productFlag: false,
                ownMaterialNumber: materialNumber,
                materialNumberSupplier: materialNumber,
                materialNumberCustomer: null,
                materialNumberCx: null,
                name: material?.name ?? '',
            };
            p.measurementUnit ??= 'unit:piece';
            dispatch({ type: 'production', payload: p });
            dispatch({ type: 'productionDialogOptions', payload: { open: true, mode } });
        },
        [material?.name, materialNumber]
    );

    const openDeliveryDialog = useCallback(
        (d: Partial<Delivery>, mode: ModalMode, direction: 'incoming' | 'outgoing' = 'outgoing', site: Site | null) => {
            d.ownMaterialNumber = materialNumber;
            dispatch({ type: 'delivery', payload: d });
            dispatch({ type: 'deliveryDialogOptions', payload: { open: true, mode, direction, site } });
        },
        [materialNumber]
    );

    const openDialog = useCallback(
        <TCategory extends DataCategory>(
            category: TCategory,
            data: Partial<DataCategoryTypeMap[TCategory]>,
            mode: ModalMode,
            direction?: 'incoming' | 'outgoing',
            site?: Site | null
        ) => {
            if (category === 'delivery') {
                openDeliveryDialog(data as Partial<DataCategoryTypeMap['delivery']>, mode, direction, site ?? null);
            } else if (category === 'demand') {
                openDemandDialog(data as Partial<DataCategoryTypeMap['demand']>, mode);
            } else if (category === 'production') {
                openProductionDialog(data as Partial<DataCategoryTypeMap['production']>, mode);
            }
        },
        [openDeliveryDialog, openDemandDialog, openProductionDialog]
    );
    return (
        <>
            <dataModalContext.Provider value={{ openDialog, addOnSaveListener }}>{children}</dataModalContext.Provider>
            <DemandCategoryModal
                {...state.demandDialogOptions}
                onClose={() => dispatch({ type: 'demandDialogOptions', payload: { open: false, mode: state.demandDialogOptions.mode } })}
                onSave={() => onSave('demand')}
                demand={state.demand}
                demands={[]}
            />
            <PlannedProductionModal
                {...state.productionDialogOptions}
                onClose={() =>
                    dispatch({ type: 'productionDialogOptions', payload: { open: false, mode: state.productionDialogOptions.mode } })
                }
                onSave={() => onSave('production')}
                production={state.production}
                productions={[]}
            />
            <DeliveryInformationModal
                {...state.deliveryDialogOptions}
                onClose={() => dispatch({ type: 'deliveryDialogOptions', payload: { ...state.deliveryDialogOptions, open: false } })}
                onSave={() => onSave('delivery')}
                delivery={state.delivery}
                deliveries={[]}
            />
        </>
    );
};

export function useDataModal() {
    const context = useContext(dataModalContext);
    if (context === null) {
        throw new Error('useDataModal must be used within a DataModalProvider');
    }
    return context;
}

type DashboardState = {
    deliveryDialogOptions: { open: boolean; mode: ModalMode; direction: 'incoming' | 'outgoing'; site: Site | null };
    demandDialogOptions: { open: boolean; mode: ModalMode };
    productionDialogOptions: { open: boolean; mode: ModalMode };
    delivery: Delivery | null;
    demand: Partial<Demand> | null;
    production: Partial<Production> | null;
};

type DashboardAction = {
    type: keyof DashboardState;
    payload: DashboardState[keyof DashboardState];
};

const reducer = (state: DashboardState, action: DashboardAction): DashboardState => {
    return { ...state, [action.type]: action.payload };
};

const initialState: DashboardState = {
    deliveryDialogOptions: { open: false, mode: 'edit', direction: 'incoming', site: null },
    demandDialogOptions: { open: false, mode: 'edit' },
    productionDialogOptions: { open: false, mode: 'edit' },
    delivery: null,
    demand: null,
    production: null,
};
