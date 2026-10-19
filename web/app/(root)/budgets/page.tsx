import HeaderBox from '@/components/HeaderBox'
import StatCard from '@/components/StatCard'
import { Card } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Badge } from '@/components/ui/badge'
import MonthlyTrendChart from '@/components/MonthlyTrendChart'
import CategoryBreakdown from '@/components/CategoryBreakdown'
import SampleDataNotice from '@/components/SampleDataNotice'
import { formatMoney } from '@/lib/money'
import {
  SAMPLE_BUDGET_LIMITS,
  SAMPLE_CATEGORY_SPEND,
  SAMPLE_CURRENCY,
  SAMPLE_MONTHLY_TREND,
  SAMPLE_THIS_MONTH,
} from '@/lib/sampleData'
import Wallet01Icon from '@hugeicons/core-free-icons/Wallet01Icon'
import ArrowDownLeft01Icon from '@hugeicons/core-free-icons/ArrowDownLeft01Icon'
import ArrowUpRight02Icon from '@hugeicons/core-free-icons/ArrowUpRight02Icon'

const TINT_INDICATOR = { blue: 'bg-blue-600', success: 'bg-success-600', pink: 'bg-pink-600' } as const;

const Budgets = () => {
  return (
    <div className="my-banks">
      <HeaderBox title="Budgets & Insights" subtext="See where your money goes, month to month." />

      <SampleDataNotice>
        This page previews Horizon&apos;s budgeting view with sample numbers — the budgets backend
        isn&apos;t built yet. Once it is, these cards show your real spending.
      </SampleDataNotice>

      <section className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard
          icon={Wallet01Icon}
          label="This month's income"
          value={formatMoney(SAMPLE_THIS_MONTH.incomeMinor, SAMPLE_CURRENCY)}
          tint="blue"
          delta={{ direction: 'up', label: '12%' }}
        />
        <StatCard
          icon={ArrowUpRight02Icon}
          label="This month's spending"
          value={formatMoney(SAMPLE_THIS_MONTH.spendingMinor, SAMPLE_CURRENCY)}
          tint="pink"
          delta={{ direction: 'down', label: '8%' }}
        />
        <StatCard
          icon={ArrowDownLeft01Icon}
          label="Net this month"
          value={formatMoney(SAMPLE_THIS_MONTH.netMinor, SAMPLE_CURRENCY)}
          tint="success"
        />
      </section>

      <section className="grid grid-cols-1 gap-6 lg:grid-cols-5">
        <Card className="flex min-w-0 flex-col gap-4 p-6 lg:col-span-3">
          <div>
            <h2 className="header-2">Income vs. spending</h2>
            <p className="text-14 text-gray-500">Last 6 months</p>
          </div>
          <div className="h-64 min-w-0">
            <MonthlyTrendChart data={SAMPLE_MONTHLY_TREND} currency={SAMPLE_CURRENCY} />
          </div>
        </Card>

        <Card className="flex min-w-0 flex-col gap-4 p-6 lg:col-span-2">
          <div>
            <h2 className="header-2">Spending by category</h2>
            <p className="text-14 text-gray-500">This month</p>
          </div>
          <CategoryBreakdown items={SAMPLE_CATEGORY_SPEND} currency={SAMPLE_CURRENCY} />
        </Card>
      </section>

      <section>
        <Card className="flex flex-col gap-5 p-6">
          <div>
            <h2 className="header-2">Budget limits</h2>
            <p className="text-14 text-gray-500">This month, by category</p>
          </div>

          <div className="flex flex-col gap-5">
            {SAMPLE_CATEGORY_SPEND.map((category) => {
              const limit = SAMPLE_BUDGET_LIMITS.find((l) => l.name === category.name);
              if (!limit) return null;
              const pct = Math.round((category.amountMinor / limit.limitMinor) * 100);
              const overBudget = pct > 100;

              return (
                <div key={category.name} className="flex flex-col gap-2">
                  <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex items-center gap-2">
                      <p className="text-14 font-semibold text-gray-900">{category.name}</p>
                      {overBudget && (
                        <Badge className="shrink-0 whitespace-nowrap border-transparent bg-pink-25 text-10 uppercase tracking-wide text-pink-700 hover:bg-pink-25">
                          Over budget
                        </Badge>
                      )}
                    </div>
                    <p className="text-12 text-gray-500">
                      {formatMoney(category.amountMinor, SAMPLE_CURRENCY)} of{' '}
                      {formatMoney(limit.limitMinor, SAMPLE_CURRENCY)}
                    </p>
                  </div>
                  <Progress
                    value={Math.min(100, pct)}
                    className="h-2 bg-gray-100"
                    indicatorClassName={overBudget ? 'bg-pink-600' : TINT_INDICATOR[category.tint]}
                  />
                </div>
              );
            })}
          </div>
        </Card>
      </section>
    </div>
  )
}

export default Budgets
