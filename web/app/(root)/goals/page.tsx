import HeaderBox from '@/components/HeaderBox'
import GoalCard from '@/components/GoalCard'
import StatCard from '@/components/StatCard'
import SampleDataNotice from '@/components/SampleDataNotice'
import { formatMoney } from '@/lib/money'
import { SAMPLE_CURRENCY, SAMPLE_GOALS } from '@/lib/sampleData'
import PiggyBankIcon from '@hugeicons/core-free-icons/PiggyBankIcon'
import Target02Icon from '@hugeicons/core-free-icons/Target02Icon'
import TrophyIcon from '@hugeicons/core-free-icons/TrophyIcon'

const Goals = () => {
  const totalSavedMinor = SAMPLE_GOALS.reduce((sum, g) => sum + g.savedMinor, 0)
  const reachedCount = SAMPLE_GOALS.filter((g) => g.savedMinor >= g.targetMinor).length
  const activeCount = SAMPLE_GOALS.length - reachedCount

  return (
    <div className="my-banks">
      <HeaderBox title="Goals" subtext="Save toward something specific." />

      <SampleDataNotice>
        Goals aren&apos;t wired to a real backend yet — these are sample targets so you can see
        what the feature will look like.
      </SampleDataNotice>

      <section className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard
          icon={PiggyBankIcon}
          label="Total saved"
          value={formatMoney(totalSavedMinor, SAMPLE_CURRENCY)}
          tint="blue"
        />
        <StatCard
          icon={Target02Icon}
          label="Active goals"
          value={String(activeCount)}
          tint="pink"
        />
        <StatCard
          icon={TrophyIcon}
          label="Goals reached"
          value={String(reachedCount)}
          tint="success"
        />
      </section>

      <section className="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
        {SAMPLE_GOALS.map((goal) => (
          <GoalCard
            key={goal.id}
            name={goal.name}
            targetMinor={goal.targetMinor}
            savedMinor={goal.savedMinor}
            targetDate={goal.targetDate}
            currency={SAMPLE_CURRENCY}
          />
        ))}
      </section>
    </div>
  )
}

export default Goals
