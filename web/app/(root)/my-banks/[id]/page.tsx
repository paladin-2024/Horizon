import { cookies } from 'next/headers'
import Link from 'next/link'
import { notFound } from 'next/navigation'
import BankCard from '@/components/BankCard'
import EmptyState from '@/components/EmptyState'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import Icon from '@/components/Icon'
import { cookieHeader, fetchAccount } from '@/lib/api/server'
import { formatMoney } from '@/lib/money'
import ArrowLeft01Icon from '@hugeicons/core-free-icons/ArrowLeft01Icon'

const AccountDetail = async ({ params }: { params: Promise<{ id: string }> }) => {
  const { id } = await params
  const header = cookieHeader((await cookies()).getAll())
  const account = await fetchAccount(header, id)
  if (!account) notFound()

  return (
    <div className="my-banks">
      <Link href="/my-banks" className="flex w-fit items-center gap-2 text-14 font-semibold text-gray-600 hover:text-gray-900">
        <Icon icon={ArrowLeft01Icon} size={18} />
        My Banks
      </Link>

      <section className="grid grid-cols-1 gap-6 lg:grid-cols-5">
        <div className="lg:col-span-2">
          <BankCard account={account} />
        </div>

        <Card className="flex flex-col gap-4 p-6 lg:col-span-3">
          <h2 className="header-2">Account details</h2>
          <dl className="grid grid-cols-2 gap-x-4 gap-y-4">
            <div>
              <dt className="text-12 text-gray-500">Institution</dt>
              <dd className="text-14 font-semibold text-gray-900">{account.institution?.name ?? "Unknown"}</dd>
            </div>
            <div>
              <dt className="text-12 text-gray-500">Status</dt>
              <dd>
                <Badge className="border-transparent bg-success-25 text-success-700 hover:bg-success-25">
                  {account.status === "ACTIVE" ? "Active" : "Archived"}
                </Badge>
              </dd>
            </div>
            <div>
              <dt className="text-12 text-gray-500">Account number</dt>
              <dd className="text-14 font-semibold text-gray-900">•••• {account.accountMask}</dd>
            </div>
            <div>
              <dt className="text-12 text-gray-500">Provider</dt>
              <dd className="text-14 font-semibold text-gray-900">{account.provider === "MANUAL" ? "Manually added" : account.provider}</dd>
            </div>
            <div>
              <dt className="text-12 text-gray-500">Current balance</dt>
              <dd className="text-18 font-semibold text-gray-900">{formatMoney(account.balance.amountMinor, account.balance.currency)}</dd>
            </div>
            <div>
              <dt className="text-12 text-gray-500">Balance as of</dt>
              <dd className="text-14 font-semibold text-gray-900">
                {new Date(account.balanceAsOf).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}
              </dd>
            </div>
          </dl>
        </Card>
      </section>

      <section className="recent-transactions">
        <h2 className="recent-transactions-label">Transactions</h2>
        <EmptyState
          illustration="/illustrations/empty-transactions.svg"
          title="No transactions yet"
          description="Transaction history is coming soon for this account."
        />
      </section>
    </div>
  )
}

export default AccountDetail
