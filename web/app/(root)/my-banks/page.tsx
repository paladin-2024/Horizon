import HeaderBox from '@/components/HeaderBox'
import EmptyState from '@/components/EmptyState'

const MyBanks = () => {
  return (
    <div className="my-banks">
      <HeaderBox
        title="My Banks"
        subtext="Manage the accounts you've linked to Horizon."
      />
      <EmptyState
        illustration="/illustrations/empty-banks.svg"
        title="No banks linked yet"
        description="Connect a bank or mobile money account to start tracking your balance here."
      />
    </div>
  )
}

export default MyBanks
