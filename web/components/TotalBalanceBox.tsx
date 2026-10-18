import DoughnutsChart from './DoughnutChart'
import AnimatedCounter from './AnimatedCounter'
import { formatMoney } from '@/lib/money'

const TotalBalanceBox = ({ accounts = [], totals = [] }: TotalBalanceBoxProps) => {
  // The largest-value currency leads the chart; every currency still gets its own total below.
  // Never summed across currencies — see DESIGN.md's "Do keep multi-currency amounts distinct" rule.
  const primary = totals.length
    ? [...totals].sort((a, b) => b.amountMinor - a.amountMinor)[0]
    : null;
  const primaryAccounts = primary ? accounts.filter((a) => a.balance.currency === primary.currency) : [];
  const otherTotals = primary ? totals.filter((t) => t.currency !== primary.currency) : [];

  return (
    <div className="relative flex w-full flex-col gap-6 overflow-hidden rounded-xl bg-bank-gradient p-6 shadow-creditCard sm:flex-row sm:items-center sm:gap-8 sm:p-8">
      <div
        aria-hidden
        className="pointer-events-none absolute -right-16 -top-16 size-64 rounded-full bg-white/10 blur-2xl"
      />
      <div className="relative flex flex-1 flex-col gap-3">
        <p className="text-14 font-medium text-white/70">
          {primary ? `Total balance (${primary.currency})` : "Total balance"}
        </p>

        <div className="flex items-center gap-2 text-36 font-bold text-white">
          {primary ? (
            <AnimatedCounter amountMinor={primary.amountMinor} currency={primary.currency} tone="dark" />
          ) : (
            <span className="text-white/60">No accounts linked yet</span>
          )}
        </div>

        <p className="text-14 font-medium text-white/70">
          {accounts.length} linked account{accounts.length === 1 ? "" : "s"}
        </p>

        {otherTotals.length > 0 && (
          <div className="flex flex-wrap gap-2 pt-2">
            {otherTotals.map((total) => (
              <span
                key={total.currency}
                className="rounded-full bg-white/15 px-3 py-1 text-12 font-semibold text-white"
              >
                {formatMoney(total.amountMinor, total.currency)}
              </span>
            ))}
          </div>
        )}
      </div>

      <div className="relative flex size-32 shrink-0 items-center justify-center self-center sm:size-36">
        <DoughnutsChart accounts={primaryAccounts} tone="dark" />
      </div>
    </div>
  )
}

export default TotalBalanceBox
