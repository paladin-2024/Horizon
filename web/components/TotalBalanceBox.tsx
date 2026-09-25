
import React from 'react'
import DoughnutsChart from './DoughnutChart'
import AnimatedCounter from './AnimatedCounter'
import { Card } from './ui/card'

const TotalBalanceBox = ({
    accounts= [],totalBanks , totalCurrentBalance}:TotlaBalanceBoxProps
  ) => {
  return (
    <Card className="total-balance">
      <div className="total-balance-chart">
        <DoughnutsChart
        accounts={accounts}
        />
      </div>

      <div className='flex flex-col gap-6'>
        <h2 className='header-2'>
          Bank Accounts: {totalBanks}
        </h2>
        <div className='flex flex-col gap-2'>
          <p className='total-balance-label'>
            Total Current balance
          </p>

          <div className='total-balance-amount flex-center gap-2'>
            <AnimatedCounter
            amount={totalCurrentBalance}
            />
          </div>
        </div>
      </div>
    </Card>
  )
}

export default TotalBalanceBox
