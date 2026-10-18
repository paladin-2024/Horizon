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
      <div className="relative flex flex-1 flex-col gap-1">
        <p className="text-14 font-medium text-white/70">
          Total balance · {accounts.length} linked account{accounts.length === 1 ? "" : "s"}
        </p>

        <div className="flex items-center gap-2 text-36 font-bold text-white">
          {primary ? (
            <AnimatedCounter amountMinor={primary.amountMinor} currency={primary.currency} tone="dark" />
          ) : (
            <span className="text-white/60">No accounts linked yet</span>
          )}
        </div>

        {otherTotals.length > 0 && (
          <div className="mt-3 flex flex-col gap-1.5 border-t border-white/15 pt-3">
            <p className="text-10 font-semibold uppercase tracking-wide text-white/50">
              Also in {otherTotals.length === 1 ? "another currency" : "other currencies"}
            </p>
            {otherTotals.map((total) => (
              <p key={total.currency} className="text-16 font-semibold text-white/90">
                {formatMoney(total.amountMinor, total.currency)}
              </p>
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
