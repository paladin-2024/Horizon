import { cookies } from 'next/headers'
import HeaderBox from '@/components/HeaderBox'
import EmptyState from '@/components/EmptyState'
import AccountChipRow from '@/components/AccountChipRow'
import { cookieHeader, fetchAccounts } from '@/lib/api/server'

const TransactionHistory = async () => {
  const header = cookieHeader((await cookies()).getAll())
  const { items: accounts } = await fetchAccounts(header)

  return (
    <div className="transactions">
      <HeaderBox
        title="Transaction History"
        subtext="See a record of every payment and transfer."
      />
      <AccountChipRow accounts={accounts} />
      <EmptyState
        illustration="/illustrations/empty-transactions.svg"
        title="No transactions yet"
        description={
          accounts.length > 0
            ? "Your linked accounts are ready — transaction history is coming in a future update."
            : "Once you link a bank and start spending, your history will show up here."
        }
      />
    </div>
  )
}

export default TransactionHistory
