import HeaderBox from '@/components/HeaderBox'
import EmptyState from '@/components/EmptyState'

const PaymentTransfer = () => {
  return (
    <div className="transactions">
      <HeaderBox
        title="Transfer Funds"
        subtext="Move money between your linked accounts."
      />
      <EmptyState
        illustration="/illustrations/transfer-coming-soon.svg"
        title="Transfers are coming soon"
        description="Link a bank first — transfers will open up once your accounts are connected."
      />
    </div>
  )
}

export default PaymentTransfer
