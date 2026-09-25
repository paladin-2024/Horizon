import Link from 'next/link'
import HeaderBox from '@/components/HeaderBox'
import RightSideBar from '@/components/RightSideBar';
import TotalBalanceBox from '@/components/TotalBalanceBox';
import EmptyState from '@/components/EmptyState';
import Icon from '@/components/Icon';
import { Button } from '@/components/ui/button';
import { mockCurrentUser } from '@/lib/session';
import BankIcon from '@hugeicons/core-free-icons/BankIcon';
import SentIcon from '@hugeicons/core-free-icons/SentIcon';

function Home() {
    const loggedIn = mockCurrentUser;
    return (
    <section className='home'>
        <div className='home-content'>
            <header className='home-header'>
                <HeaderBox
                    type="greeting"
                    title="Welcome"
                    user={loggedIn?.firstName || 'Guest'}
                    subtext="Access and manage your account and transactions efficiently."
                />

                <TotalBalanceBox
                accounts={[]}
                totalBanks={1}
                totalCurrentBalance={1250.35}
                />
            </header>

            <section className="flex flex-wrap gap-3">
                <Button asChild variant="outline" className="gap-2">
                    <Link href="/my-banks">
                        <Icon icon={BankIcon} size={18} />
                        Add a bank
                    </Link>
                </Button>
                <Button asChild variant="outline" className="gap-2">
                    <Link href="/payment-transfer">
                        <Icon icon={SentIcon} size={18} />
                        Transfer funds
                    </Link>
                </Button>
            </section>

            <section className="recent-transactions">
                <h2 className="recent-transactions-label">Recent transactions</h2>
                <EmptyState
                    illustration="/illustrations/empty-transactions.svg"
                    title="No transactions yet"
                    description="Link a bank to start seeing your activity here."
                />
            </section>
        </div>
        <RightSideBar
            user={loggedIn}
            transactions={[]}
            banks={[{currentBalance:123.50}]}
        />
    </section>
    )
}

export default Home
