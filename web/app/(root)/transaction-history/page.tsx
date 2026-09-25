import HeaderBox from '@/components/HeaderBox'
import EmptyState from '@/components/EmptyState'

const TransactionHistory = () => {
  return (
    <div className="transactions">
      <HeaderBox
        title="Transaction History"
        subtext="See a record of every payment and transfer."
      />
      <EmptyState
        illustration="/illustrations/empty-transactions.svg"
        title="No transactions yet"
        description="Once you link a bank and start spending, your history will show up here."
      />
    </div>
  )
}

export default TransactionHistory
