import { createContext, ReactNode, useContext, useEffect, useState } from 'react';

type TitleContext = {
    title: string;
    setTitle: (title: string) => void
};

const titleContext = createContext<TitleContext | null>(null);

type TitleContextProviderProps = {
    children: ReactNode;
};

export const TitleContextProvider = ({ children }: TitleContextProviderProps) => {
    const [title, setTitle] = useState<string>(() => 'PURIS');
    useEffect(() => {
      document.title = title;
    }, [title])
    const changeTitle = (title: string) => {
      setTitle(`${title} | PURIS`)
    }
    return (
        <>
            <titleContext.Provider value={{ title, setTitle: changeTitle }}>{children}</titleContext.Provider>
        </>
    );
};

export function useTitle() {
    const context = useContext(titleContext);
    if (context === null) {
        throw new Error('useTitle must be used within a TitleContextProvider');
    }
    return context;
}
