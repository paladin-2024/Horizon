import { cookies } from 'next/headers'
import Link from 'next/link'
import HeaderBox from '@/components/HeaderBox'
import { Card } from '@/components/ui/card'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import Icon from '@/components/Icon'
import SignOutButton from '@/components/SignOutButton'
import { cookieHeader, fetchAccounts, fetchMe } from '@/lib/api/server'
import { formatMoney } from '@/lib/money'
import Mail01Icon from '@hugeicons/core-free-icons/Mail01Icon'
import SmartPhone01Icon from '@hugeicons/core-free-icons/SmartPhone01Icon'
import Location01Icon from '@hugeicons/core-free-icons/Location01Icon'

const COUNTRY_NAME: Record<string, string> = { UG: 'Uganda', CD: 'DR Congo' }

const Profile = async () => {
  const header = cookieHeader((await cookies()).getAll())
  const [user, { items: accounts }] = await Promise.all([fetchMe(header), fetchAccounts(header)])
  if (!user) return null

  return (
    <div className="my-banks">
      <HeaderBox title="Profile & Settings" subtext="Your account and linked accounts." />

      <section className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="flex flex-col items-start gap-5 rounded-xl border border-gray-200 p-6 shadow-chart lg:col-span-1">
          <Avatar className="size-16">
            <AvatarFallback className="bg-bank-gradient text-20 font-semibold text-white">
              {user.firstName[0]}
            </AvatarFallback>
          </Avatar>
          <div>
            <h2 className="text-18 font-semibold text-gray-900">
              {user.firstName} {user.lastName}
            </h2>
            <p className="text-14 text-gray-500">Horizon member</p>
          </div>

          <div className="flex w-full flex-col gap-3 border-t border-gray-200 pt-4">
            <div className="flex items-center gap-3">
              <Icon icon={SmartPhone01Icon} size={18} className="text-gray-500" />
              <p className="text-14 text-gray-700">{user.phone}</p>
            </div>
            {user.email && (
              <div className="flex items-center gap-3">
                <Icon icon={Mail01Icon} size={18} className="text-gray-500" />
                <p className="text-14 text-gray-700">{user.email}</p>
              </div>
            )}
            <div className="flex items-center gap-3">
              <Icon icon={Location01Icon} size={18} className="text-gray-500" />
              <p className="text-14 text-gray-700">{COUNTRY_NAME[user.country] ?? user.country}</p>
            </div>
          </div>

          <SignOutButton />
        </Card>

        <Card className="flex flex-col gap-4 rounded-xl border border-gray-200 p-6 shadow-chart lg:col-span-2">
          <div className="flex items-center justify-between">
            <h2 className="header-2">Linked accounts ({accounts.length})</h2>
            <Link href="/my-banks" className="text-14 font-semibold text-bankGradient">
              Manage
            </Link>
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

          <div className="flex items-center justify-between rounded-lg border border-gray-200 bg-gray-25 px-4 py-3">
            <div>
              <p className="text-14 font-semibold text-gray-700">Notification preferences</p>
              <p className="text-12 text-gray-500">Low-balance and large-transaction alerts</p>
            </div>
            <span className="rounded-full bg-gray-200 px-2.5 py-1 text-10 font-semibold uppercase tracking-wide text-gray-600">
              Coming soon
            </span>
          </div>
        </Card>
      </section>
    </div>
  )
}

export default Profile
