export interface Member {
  id: string;
  name: string;
  order: number; // 0-based index for payout month
}

export interface PaymentStatus {
  [memberId: string]: boolean; // true if paid
}

export interface MonthRecord {
  monthIndex: number;
  payments: PaymentStatus;
  payoutCompleted: boolean; // Did the recipient receive the pot?
}

export interface AssociationData {
  id: string;
  name: string;
  startDate: string; // ISO Date string (YYYY-MM)
  amountPerPerson: number;
  members: Member[];
  currentMonthIndex: number;
  history: { [monthIndex: number]: MonthRecord };
}

export enum AppScreen {
  SETUP = 'SETUP',
  DASHBOARD = 'DASHBOARD'
}