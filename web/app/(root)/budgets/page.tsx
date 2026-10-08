import HeaderBox from '@/components/HeaderBox'
import StatCard from '@/components/StatCard'
import { Card } from '@/components/ui/card'
import MonthlyTrendChart from '@/components/MonthlyTrendChart'
import CategoryBreakdown from '@/components/CategoryBreakdown'
import SampleDataNotice from '@/components/SampleDataNotice'
import { formatMoney } from '@/lib/money'
import { SAMPLE_CATEGORY_SPEND, SAMPLE_CURRENCY, SAMPLE_MONTHLY_TREND, SAMPLE_THIS_MONTH } from '@/lib/sampleData'
import Wallet01Icon from '@hugeicons/core-free-icons/Wallet01Icon'
import ArrowDownLeft01Icon from '@hugeicons/core-free-icons/ArrowDownLeft01Icon'
import ArrowUpRight02Icon from '@hugeicons/core-free-icons/ArrowUpRight02Icon'

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
        <Card className="flex flex-col gap-4 rounded-xl border border-gray-200 p-6 shadow-chart lg:col-span-3">
          <div>
            <h2 className="header-2">Income vs. spending</h2>
            <p className="text-14 text-gray-500">Last 6 months</p>
          </div>
          <div className="h-64">
            <MonthlyTrendChart data={SAMPLE_MONTHLY_TREND} currency={SAMPLE_CURRENCY} />
          </div>
        </Card>

        <Card className="flex flex-col gap-4 rounded-xl border border-gray-200 p-6 shadow-chart lg:col-span-2">
          <div>
            <h2 className="header-2">Spending by category</h2>
            <p className="text-14 text-gray-500">This month</p>
          </div>
          <CategoryBreakdown items={SAMPLE_CATEGORY_SPEND} currency={SAMPLE_CURRENCY} />
        </Card>
      </section>
    </div>
  )
}

export default Budgets
