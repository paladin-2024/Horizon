import { cookies } from 'next/headers'
import HeaderBox from '@/components/HeaderBox'
import EmptyState from '@/components/EmptyState'
import AccountChipRow from '@/components/AccountChipRow'
import { cookieHeader, fetchAccounts } from '@/lib/api/server'

const PaymentTransfer = async () => {
  const header = cookieHeader((await cookies()).getAll())
  const { items: accounts } = await fetchAccounts(header)

  return (
    <div className="transactions">
      <HeaderBox
        title="Transfer Funds"
        subtext="Move money between your linked accounts."
      />
      <AccountChipRow accounts={accounts} />
      <EmptyState
        illustration="/illustrations/transfer-coming-soon.svg"
        title="Transfers are coming soon"
        description={
          accounts.length > 0
            ? "You'll be able to move money between the accounts above once transfers ship."
            : "Link a bank first — transfers will open up once your accounts are connected."
        }
      />
    </div>
  )
}

export default PaymentTransfer
