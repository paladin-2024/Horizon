import Link from 'next/link'
import { cookies } from 'next/headers'
import HeaderBox from '@/components/HeaderBox'
import RightSideBar from '@/components/RightSideBar';
import TotalBalanceBox from '@/components/TotalBalanceBox';
import EmptyState from '@/components/EmptyState';
import Icon from '@/components/Icon';
import { Button } from '@/components/ui/button';
import { cookieHeader, fetchAccounts, fetchMe } from '@/lib/api/server';
import BankIcon from '@hugeicons/core-free-icons/BankIcon';
import SentIcon from '@hugeicons/core-free-icons/SentIcon';

async function Home() {
    const cookieStore = await cookies();
    const header = cookieHeader(cookieStore.getAll());
    // The (root) layout already confirmed the session; a second /auth/me call
    // here is cheap and keeps this page self-contained if it's ever reused.
    const [loggedIn, accountList] = await Promise.all([fetchMe(header), fetchAccounts(header)]);
    const { items: accounts, totals } = accountList;

    return (
    <section className='home'>
        <div className='home-content'>
            <header className='home-header'>
                <HeaderBox
                    type="greeting"
                    title="Welcome"
                    user={loggedIn?.firstName || 'there'}
                    subtext="Access and manage your account and transactions efficiently."
                />

                <TotalBalanceBox
                    accounts={accounts}
                    totals={totals}
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
                    description={
                        accounts.length > 0
                            ? "Transaction history is coming soon — your linked accounts are ready for it."
                            : "Link a bank to start seeing your activity here."
                    }
                />
            </section>
        </div>
        {loggedIn && <RightSideBar user={loggedIn} accounts={accounts} />}
    </section>
    )
}

export default Home
