import Icon from "./Icon";
import BankIcon from "@hugeicons/core-free-icons/BankIcon";

/** A compact real-account context strip for pages that don't have their own data yet. */
const AccountChipRow = ({ accounts }: { accounts: ApiAccount[] }) => {
  if (accounts.length === 0) return null;

  return (
    <div className="flex flex-wrap gap-2">
      {accounts.map((a) => (
        <span
          key={a.id}
          className="flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-2 text-12 font-semibold text-gray-700"
        >
          <Icon icon={BankIcon} size={14} className="text-gray-500" />
          {a.displayName}
          <span className="text-gray-400">•••• {a.accountMask}</span>
        </span>
      ))}
    </div>
  );
};

export default AccountChipRow;
