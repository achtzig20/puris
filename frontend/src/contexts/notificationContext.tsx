import { createContext, ReactNode, useContext, useEffect, useState } from 'react';
import { Notification } from '@models/types/data/notification';
import { PageSnackbar, PageSnackbarStack } from '@catena-x/portal-shared-components';

type NotificationContext = {
    notify: (notification: Notification) => void;
};

const notificationContext = createContext<NotificationContext | null>(null);

type NotificationProviderProps = {
    children: ReactNode;
};

export const NotificationContextProvider = ({ children }: NotificationProviderProps) => {
    const [notifications, setNotifications] = useState<Notification[]>([]);
  const notify = (notification: Notification) => {
    setNotifications(ns => [...ns, notification]);
  }
    return (
        <>
            <notificationContext.Provider value={{notify}}>{children}</notificationContext.Provider>
            <PageSnackbarStack>
                {notifications.map((notification, index) => (
                    <PageSnackbar
                        key={index}
                        open={!!notification}
                        severity={notification?.severity}
                        title={notification?.title}
                        description={notification?.description}
                        autoClose={true}
                        onCloseNotification={() => setNotifications((ns) => ns.filter((_, i) => i !== index) ?? [])}
                    />
                ))}
            </PageSnackbarStack>
        </>
    );
};

export function useNotifications() {
  const context = useContext(notificationContext);
  if (context === null) {
    throw new Error('useNotifcations must be used within a NotificationContextProvider');
  }
  return context;
}
