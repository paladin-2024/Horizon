import Link from 'next/link'
import { Card } from './ui/card'
import { Badge } from './ui/badge'
import Icon from './Icon'
import DoughnutChart from './DoughnutChart'
import { formatMoney } from '@/lib/money'
import BankIcon from '@hugeicons/core-free-icons/BankIcon'

/**
 * The home dashboard's main panel: a real balance chart (grouped by the
 * primary currency, same rule as TotalBalanceBox) paired with a row per
 * linked account. Only rendered when there is at least one account —
 * Home falls back to the "link a bank" quick action otherwise.
 */
const AccountOverview = ({ accounts, totals }: { accounts: ApiAccount[]; totals: Money[] }) => {
  const primary = [...totals].sort((a, b) => b.amountMinor - a.amountMinor)[0];
  const primaryAccounts = accounts.filter((a) => a.balance.currency === primary.currency);

  return (
    <Card className="flex flex-col gap-6 p-6">
      <div>
        <h2 className="header-2">Account overview</h2>
        <p className="text-14 text-gray-500">Balance across every linked account</p>
      </div>

      <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
        <div className="relative size-36 shrink-0 self-center sm:size-40">
          <DoughnutChart accounts={primaryAccounts} tone="light" />
          <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
            <p className="text-10 text-gray-500">Total</p>
            <p className="text-14 font-semibold text-gray-900">{formatMoney(primary.amountMinor, primary.currency)}</p>
          </div>
        </div>

        <div className="flex flex-1 flex-col gap-3">
          {accounts.map((account) => (
            <Link
              key={account.id}
              href={`/my-banks/${account.id}`}
              className="flex items-center justify-between gap-3 rounded-lg border border-gray-100 p-3 transition-colors hover:border-gray-200 hover:bg-gray-50"
            >
              <div className="flex min-w-0 items-center gap-3">
                <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-blue-25 text-blue-700">
                  <Icon icon={BankIcon} size={20} />
                </span>
                <div className="min-w-0">
                  <p className="truncate text-14 font-semibold text-gray-900">{account.displayName}</p>
                  <p className="truncate text-12 text-gray-500">
                    {account.institution?.name ?? "Unknown"} · •••• {account.accountMask}
                  </p>
                </div>
              </div>
              <div className="shrink-0 text-right">
                <p className="text-16 font-bold text-gray-900">
                  {formatMoney(account.balance.amountMinor, account.balance.currency)}
                </p>
                <div className="flex items-center justify-end gap-1.5">
                  {account.balance.currency === primary.currency && (
                    <span className="text-10 text-gray-400">
                      {Math.round((account.balance.amountMinor / primary.amountMinor) * 100)}% of total
                    </span>
                  )}
                  <Badge
                    className={
                      account.status === "ACTIVE"
                        ? "border-transparent bg-success-25 text-10 text-success-700 hover:bg-success-25"
                        : "border-transparent bg-gray-100 text-10 text-gray-600 hover:bg-gray-100"
                    }
                  >
                    {account.status === "ACTIVE" ? "Active" : "Archived"}
                  </Badge>
                </div>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </Card>
  );
};

export default AccountOverview;
