import DoughnutsChart from './DoughnutChart'
import AnimatedCounter from './AnimatedCounter'
import { Card } from './ui/card'
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
    <Card className="total-balance">
      <div className="total-balance-chart">
        <DoughnutsChart accounts={primaryAccounts} />
      </div>

      <div className='flex flex-1 flex-col gap-6'>
        <h2 className='header-2'>
          Linked accounts: {accounts.length}
        </h2>
        <div className='flex flex-col gap-2'>
          <p className='total-balance-label'>
            {primary ? `Total balance (${primary.currency})` : "Total balance"}
          </p>

          <div className='total-balance-amount flex items-center gap-2'>
            {primary ? (
              <AnimatedCounter amountMinor={primary.amountMinor} currency={primary.currency} />
            ) : (
              <span className="text-gray-400">No accounts linked yet</span>
            )}
          </div>
        </div>

        {otherTotals.length > 0 && (
          <div className="flex flex-wrap gap-x-6 gap-y-1 border-t border-gray-200 pt-3">
            {otherTotals.map((total) => (
              <p key={total.currency} className="text-14 text-gray-600">
                <span className="font-semibold text-gray-900">
                  {formatMoney(total.amountMinor, total.currency)}
                </span>
              </p>
            ))}
          </div>
        )}
      </div>
    </Card>
  )
}

export default TotalBalanceBox
