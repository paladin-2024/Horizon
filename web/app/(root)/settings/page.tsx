import { cookies } from 'next/headers'
import Link from 'next/link'
import HeaderBox from '@/components/HeaderBox'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import Icon from '@/components/Icon'
import SignOutButton from '@/components/SignOutButton'
import { cookieHeader, fetchAccounts } from '@/lib/api/server'
import { formatMoney } from '@/lib/money'
import BankIcon from '@hugeicons/core-free-icons/BankIcon'
import Notification03Icon from '@hugeicons/core-free-icons/Notification03Icon'
import ShieldCheckIcon from '@hugeicons/core-free-icons/ShieldCheckIcon'

const Settings = async () => {
  const header = cookieHeader((await cookies()).getAll())
  const { items: accounts } = await fetchAccounts(header)

  return (
    <div className="my-banks">
      <HeaderBox title="Settings" subtext="Manage your linked accounts, alerts and security." />

      <section className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card className="flex flex-col gap-4 p-6">
          <div className="flex items-center gap-3">
            <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-blue-25 text-blue-700">
              <Icon icon={BankIcon} size={20} />
            </span>
            <div>
              <h2 className="text-16 font-semibold text-gray-900">Linked accounts</h2>
              <p className="text-12 text-gray-500">{accounts.length} connected</p>
            </div>
          </div>

          {accounts.length === 0 ? (
            <p className="text-14 text-gray-500">No accounts linked yet.</p>
          ) : (
            <div className="flex flex-col divide-y divide-gray-200">
              {accounts.map((account) => (
                <div key={account.id} className="flex items-center justify-between gap-3 py-3">
                  <div className="min-w-0">
                    <p className="truncate text-14 font-semibold text-gray-900">{account.displayName}</p>
                    <p className="text-12 text-gray-500">
                      {account.institution?.name ?? 'Unknown institution'} · •••• {account.accountMask}
                    </p>
                  </div>
                  <p className="shrink-0 text-14 font-semibold text-gray-900">
                    {formatMoney(account.balance.amountMinor, account.balance.currency)}
                  </p>
                </div>
              ))}
            </div>
          )}

          <Link href="/my-banks" className="text-14 font-semibold text-bankGradient">
            Manage in My Banks
          </Link>
        </Card>

        <div className="flex flex-col gap-6">
          <Card className="flex items-center justify-between gap-4 p-6">
            <div className="flex items-center gap-3">
              <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-pink-25 text-pink-700">
                <Icon icon={Notification03Icon} size={20} />
              </span>
              <div>
                <p className="text-14 font-semibold text-gray-700">Notification preferences</p>
                <p className="text-12 text-gray-500">Low-balance and large-transaction alerts</p>
              </div>
            </div>
            <Badge className="border-transparent bg-gray-200 text-10 uppercase tracking-wide text-gray-600 hover:bg-gray-200">
              Coming soon
            </Badge>
          </Card>

          <Card className="flex flex-col gap-4 p-6">
            <div className="flex items-center gap-3">
              <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-success-25 text-success-700">
                <Icon icon={ShieldCheckIcon} size={20} />
              </span>
              <div>
                <p className="text-14 font-semibold text-gray-700">Security</p>
                <p className="text-12 text-gray-500">Sign out of Horizon on this device</p>
              </div>
            </div>
            <SignOutButton />
          </Card>
        </div>
      </section>
    </div>
  )
}

export default Settings
