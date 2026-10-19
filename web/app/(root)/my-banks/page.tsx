import { cookies } from 'next/headers'
import HeaderBox from '@/components/HeaderBox'
import EmptyState from '@/components/EmptyState'
import BankCard from '@/components/BankCard'
import AddAccountForm from '@/components/AddAccountForm'
import { cookieHeader, fetchAccounts, fetchInstitutions } from '@/lib/api/server'

const MyBanks = async () => {
  const header = cookieHeader((await cookies()).getAll())
  const [{ items: accounts }, { items: institutions }] = await Promise.all([
    fetchAccounts(header),
    fetchInstitutions(header),
  ])

  return (
    <div className="my-banks">
      <div className="flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-center">
        <HeaderBox
          title="My Banks"
          subtext="Manage the accounts you've linked to Horizon."
        />
        <AddAccountForm institutions={institutions} />
      </div>

      {accounts.length > 0 ? (
        <section className="flex flex-wrap gap-6">
          {accounts.map((account) => (
            <BankCard key={account.id} account={account} />
          ))}
        </section>
      ) : (
        <EmptyState
          illustration="/illustrations/empty-banks.svg"
          title="No banks linked yet"
          description="Link a bank or mobile money account to start tracking your balance here."
        />
      )}
    </div>
  )
}

export default MyBanks
