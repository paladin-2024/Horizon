import HeaderBox from '@/components/HeaderBox'
import GoalCard from '@/components/GoalCard'
import SampleDataNotice from '@/components/SampleDataNotice'
import { SAMPLE_CURRENCY, SAMPLE_GOALS } from '@/lib/sampleData'

const Goals = () => {
  return (
    <div className="my-banks">
      <HeaderBox title="Goals" subtext="Save toward something specific." />

      <SampleDataNotice>
        Goals aren&apos;t wired to a real backend yet — these are sample targets so you can see
        what the feature will look like.
      </SampleDataNotice>

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
