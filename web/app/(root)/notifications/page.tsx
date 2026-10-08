import HeaderBox from '@/components/HeaderBox'
import SampleDataNotice from '@/components/SampleDataNotice'
import Icon from '@/components/Icon'
import { cn } from '@/lib/utils'
import { SAMPLE_NOTIFICATIONS, SampleNotification } from '@/lib/sampleData'
import AlertCircleIcon from '@hugeicons/core-free-icons/AlertCircleIcon'
import MoneyReceiveCircleIcon from '@hugeicons/core-free-icons/MoneyReceiveCircleIcon'
import Megaphone01Icon from '@hugeicons/core-free-icons/Megaphone01Icon'

const KIND: Record<SampleNotification['kind'], { icon: typeof AlertCircleIcon; bg: string; text: string }> = {
  balance: { icon: AlertCircleIcon, bg: 'bg-pink-25', text: 'text-pink-700' },
  transaction: { icon: MoneyReceiveCircleIcon, bg: 'bg-blue-25', text: 'text-blue-700' },
  system: { icon: Megaphone01Icon, bg: 'bg-success-25', text: 'text-success-700' },
}

const Notifications = () => {
  return (
    <div className="my-banks">
      <HeaderBox title="Notifications" subtext="Balance alerts and account activity." />

      <SampleDataNotice>
        Real-time alerts need the transactions backend (coming next) to have anything to alert on
        — these are sample notifications so you can see the design.
      </SampleDataNotice>

      <section className="flex flex-col gap-3">
        {SAMPLE_NOTIFICATIONS.map((n) => {
          const { icon, bg, text } = KIND[n.kind]
          return (
            <div
              key={n.id}
              className={cn(
                'flex items-start gap-4 rounded-xl border border-gray-200 p-4 shadow-chart sm:p-5',
                !n.read && 'bg-blue-25/40'
              )}
            >
              <span className={cn('flex size-10 shrink-0 items-center justify-center rounded-full', bg)}>
                <Icon icon={icon} size={20} className={text} />
              </span>
              <div className="flex min-w-0 flex-1 flex-col gap-1">
                <div className="flex items-start justify-between gap-2">
                  <h3 className="text-14 font-semibold text-gray-900">{n.title}</h3>
                  {!n.read && <span className="mt-1 size-2 shrink-0 rounded-full bg-bankGradient" />}
                </div>
                <p className="text-14 text-gray-600">{n.description}</p>
                <p className="text-12 text-gray-400">{n.time}</p>
              </div>
            </div>
          )
        })}
      </section>
    </div>
  )
}

export default Notifications
