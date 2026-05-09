export interface Notification {
  id: number;
  recipientCin: number;
  title: string;
  message: string;
  type: 'INFO' | 'SUCCESS' | 'WARNING' | 'URGENT' | 'ERROR';
  relatedUrl?: string;
  read: boolean;
  createdAt: string;
}
