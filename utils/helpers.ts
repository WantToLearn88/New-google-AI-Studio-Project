export const formatCurrency = (amount: number) => {
  return new Intl.NumberFormat('ar-EG', {
    style: 'currency',
    currency: 'EGP',
    maximumFractionDigits: 0,
  }).format(amount);
};

export const getMonthName = (startDateStr: string, monthOffset: number) => {
  const date = new Date(startDateStr);
  date.setMonth(date.getMonth() + monthOffset);
  return new Intl.DateTimeFormat('ar-EG', { month: 'long', year: 'numeric' }).format(date);
};

export const generateId = () => {
  return Math.random().toString(36).substr(2, 9);
};