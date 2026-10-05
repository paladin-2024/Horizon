import Home09Icon from "@hugeicons/core-free-icons/Home09Icon";
import BankIcon from "@hugeicons/core-free-icons/BankIcon";
import ReceiptDollarIcon from "@hugeicons/core-free-icons/ReceiptDollarIcon";
import SentIcon from "@hugeicons/core-free-icons/SentIcon";
import PieChart01Icon from "@hugeicons/core-free-icons/PieChart01Icon";
import Target02Icon from "@hugeicons/core-free-icons/Target02Icon";
import Notification03Icon from "@hugeicons/core-free-icons/Notification03Icon";
import UserIcon from "@hugeicons/core-free-icons/UserIcon";

export const sidebarLinks = [
  {
    icon: Home09Icon,
    route: "/",
    label: "Home",
  },
  {
    icon: BankIcon,
    route: "/my-banks",
    label: "My Banks",
  },
  {
    icon: ReceiptDollarIcon,
    route: "/transaction-history",
    label: "Transaction History",
  },
  {
    icon: SentIcon,
    route: "/payment-transfer",
    label: "Transfer Funds",
  },
  {
    icon: PieChart01Icon,
    route: "/budgets",
    label: "Budgets",
  },
  {
    icon: Target02Icon,
    route: "/goals",
    label: "Goals",
  },
  {
    icon: Notification03Icon,
    route: "/notifications",
    label: "Notifications",
  },
  {
    icon: UserIcon,
    route: "/profile",
    label: "Profile & Settings",
  },
];

/** The three currencies LinkedAccountService accepts (backend/.../LinkedAccountService.java). */
export const SUPPORTED_CURRENCIES: SupportedCurrency[] = ["UGX", "CDF", "USD"];

export const topCategoryStyles = {
  "Food and Drink": {
    bg: "bg-blue-25",
    circleBg: "bg-blue-100",
    text: {
      main: "text-blue-900",
      count: "text-blue-700",
    },
    progress: {
      bg: "bg-blue-100",
      indicator: "bg-blue-700",
    },
    icon: "/icons/monitor.svg",
  },
  Travel: {
    bg: "bg-success-25",
    circleBg: "bg-success-100",
    text: {
      main: "text-success-900",
      count: "text-success-700",
    },
    progress: {
      bg: "bg-success-100",
      indicator: "bg-success-700",
    },
    icon: "/icons/coins.svg",
  },
  default: {
    bg: "bg-pink-25",
    circleBg: "bg-pink-100",
    text: {
      main: "text-pink-900",
      count: "text-pink-700",
    },
    progress: {
      bg: "bg-pink-100",
      indicator: "bg-pink-700",
    },
    icon: "/icons/shopping-bag.svg",
  },
};

export const transactionCategoryStyles = {
  "Food and Drink": {
    borderColor: "border-pink-600",
    backgroundColor: "bg-pink-500",
    textColor: "text-pink-700",
    chipBackgroundColor: "bg-inherit",
  },
  Payment: {
    borderColor: "border-success-600",
    backgroundColor: "bg-green-600",
    textColor: "text-success-700",
    chipBackgroundColor: "bg-inherit",
  },
  "Bank Fees": {
    borderColor: "border-success-600",
    backgroundColor: "bg-green-600",
    textColor: "text-success-700",
    chipBackgroundColor: "bg-inherit",
  },
  Transfer: {
    borderColor: "border-red-700",
    backgroundColor: "bg-red-700",
    textColor: "text-red-700",
    chipBackgroundColor: "bg-inherit",
  },
  Processing: {
    borderColor: "border-[#F2F4F7]",
    backgroundColor: "bg-gray-500",
    textColor: "text-[#344054]",
    chipBackgroundColor: "bg-[#F2F4F7]",
  },
  Success: {
    borderColor: "border-[#12B76A]",
    backgroundColor: "bg-[#12B76A]",
    textColor: "text-[#027A48]",
    chipBackgroundColor: "bg-[#ECFDF3]",
  },
  default: {
    borderColor: "",
    backgroundColor: "bg-blue-500",
    textColor: "text-blue-700",
    chipBackgroundColor: "bg-inherit",
  },
};
