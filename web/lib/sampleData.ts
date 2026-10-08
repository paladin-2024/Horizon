// Placeholder content for pages whose real backend module doesn't exist yet
// (budgets, goals, notifications — see PRODUCT.md "Capabilities and Constraints").
// Every page that reads from here must show the SampleDataNotice banner.
// Replace with real API data once the corresponding backend slice lands.

export const SAMPLE_CURRENCY = "UGX";

// income/spending below are minor units (×100), matching the backend's Money convention.
export const SAMPLE_MONTHLY_TREND: { month: string; income: number; spending: number }[] = [
  { month: "Apr", income: 245_000_000, spending: 168_000_000 },
  { month: "May", income: 245_000_000, spending: 192_000_000 },
  { month: "Jun", income: 260_000_000, spending: 154_000_000 },
  { month: "Jul", income: 245_000_000, spending: 201_000_000 },
  { month: "Aug", income: 280_000_000, spending: 177_000_000 },
  { month: "Sep", income: 245_000_000, spending: 143_000_000 },
];

export const SAMPLE_THIS_MONTH = {
  incomeMinor: 245_000_000,
  spendingMinor: 143_000_000,
  get netMinor() {
    return this.incomeMinor - this.spendingMinor;
  },
};

export const SAMPLE_CATEGORY_SPEND: { name: string; amountMinor: number; tint: "blue" | "success" | "pink" }[] = [
  { name: "Rent & bills", amountMinor: 65_000_000, tint: "blue" },
  { name: "Groceries", amountMinor: 31_000_000, tint: "success" },
  { name: "Transport", amountMinor: 18_000_000, tint: "pink" },
  { name: "Mobile money fees", amountMinor: 9_500_000, tint: "blue" },
  { name: "Everything else", amountMinor: 19_500_000, tint: "success" },
];

export const SAMPLE_GOALS: {
  id: string;
  name: string;
  targetMinor: number;
  savedMinor: number;
  targetDate: string;
}[] = [
  { id: "emergency", name: "Emergency fund", targetMinor: 5_000_000_00, savedMinor: 3_100_000_00, targetDate: "Dec 2026" },
  { id: "laptop", name: "New laptop", targetMinor: 2_800_000_00, savedMinor: 1_050_000_00, targetDate: "Mar 2027" },
  { id: "rent", name: "Rent deposit", targetMinor: 1_200_000_00, savedMinor: 1_200_000_00, targetDate: "Done" },
];

export type SampleNotification = {
  id: string;
  title: string;
  description: string;
  time: string;
  read: boolean;
  kind: "balance" | "transaction" | "system";
};

export const SAMPLE_NOTIFICATIONS: SampleNotification[] = [
  {
    id: "n1",
    title: "Low balance on Salary account",
    description: "Your Stanbic Bank Uganda account dropped below UGX 200,000.",
    time: "2 hours ago",
    read: false,
    kind: "balance",
  },
  {
    id: "n2",
    title: "Large transaction flagged",
    description: "A transaction over UGX 500,000 was recorded on your MTN Mobile Money wallet.",
    time: "Yesterday",
    read: false,
    kind: "transaction",
  },
  {
    id: "n3",
    title: "Welcome to Horizon",
    description: "Link your first account to start seeing your real balances here.",
    time: "3 days ago",
    read: true,
    kind: "system",
  },
];
