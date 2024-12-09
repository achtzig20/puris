import { Delivery } from '@models/types/data/delivery';
import { Production } from '@models/types/data/production';
import { Stock } from '@models/types/data/stock';
import { groupBy } from '@util/helpers';

type DailySummary = {
    productions: Production[];
    productionTotal: number;
    deliveries: Delivery[];
    deliveryTotal: number;
    stocks: Stock[];
    stockTotal: number;
};

export type ProductionSummary = Record<string, DailySummary>;

export function createSummary(productions: Production[], deliveries: Delivery[], stocks: Stock[], timespan: number = 28) {
    const groupedProductions = groupBy(productions, (item) => new Date(item.estimatedTimeOfCompletion).toLocaleDateString());
    const groupedDeliveries = groupBy(deliveries, (item) => new Date(item.dateOfDeparture).toLocaleDateString());
    const summary: ProductionSummary = {};
    const dates = [...new Array(timespan).keys()].map((index) => {
        const today = new Date();
        today.setDate(today.getDate() + index);
        return today.toLocaleDateString();
    });
    for (let i = 0; i <= timespan; i++) {
        const dateString = dates[i];
        const previousDateString = i !== 0 ? dates[i - 1] : '';
        const dailySummary: DailySummary = {
            productions: groupedProductions[dateString] ?? [],
            productionTotal: (groupedProductions[dateString] ?? []).reduce((sum, p) => sum + p.quantity, 0),
            deliveries: groupedDeliveries[dateString] ?? [],
            deliveryTotal: (groupedDeliveries[dateString] ?? []).reduce((sum, d) => sum + d.quantity, 0),
            stocks: i === 0 ? stocks ?? [] : [],
            stockTotal:
                i === 0
                    ? stocks.reduce((sum, s) => sum + s.quantity, 0)
                    : summary[previousDateString].stockTotal +
                      summary[previousDateString].productionTotal -
                      summary[previousDateString].deliveryTotal,
        };
        summary[dateString] = dailySummary;
    }
    return summary;
}
