import { cookies } from 'next/headers'
import HeaderBox from '@/components/HeaderBox'
import RightSideBar from '@/components/RightSideBar';
import TotalBalanceBox from '@/components/TotalBalanceBox';
import EmptyState from '@/components/EmptyState';
import QuickAction from '@/components/QuickAction';
import StatCard from '@/components/StatCard';
import AccountOverview from '@/components/AccountOverview';
import RefreshButton from '@/components/RefreshButton';
import { cookieHeader, fetchAccounts, fetchMe } from '@/lib/api/server';
import BankIcon from '@hugeicons/core-free-icons/BankIcon';
import SentIcon from '@hugeicons/core-free-icons/SentIcon';
import PieChart01Icon from '@hugeicons/core-free-icons/PieChart01Icon';
import UserIcon from '@hugeicons/core-free-icons/UserIcon';
import CheckmarkCircle02Icon from '@hugeicons/core-free-icons/CheckmarkCircle02Icon';
import Exchange02Icon from '@hugeicons/core-free-icons/Exchange02Icon';

async function Home() {
    const cookieStore = await cookies();
    const header = cookieHeader(cookieStore.getAll());
    // The (root) layout already confirmed the session; a second /auth/me call
    // here is cheap and keeps this page self-contained if it's ever reused.
    const [loggedIn, accountList] = await Promise.all([fetchMe(header), fetchAccounts(header)]);
    const { items: accounts, totals } = accountList;
    const activeCount = accounts.filter((a) => a.status === 'ACTIVE').length;

    return (
    <section className='home'>
        <div className='home-content'>
            <header className='home-header'>
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <HeaderBox
                        type="greeting"
                        title="Welcome"
                        user={loggedIn?.firstName || 'there'}
                        subtext="Access and manage your account and transactions efficiently."
                    />
                    <RefreshButton />
                </div>

                <TotalBalanceBox
                    accounts={accounts}
                    totals={totals}
                />
            </header>

            {accounts.length > 0 && (
                <>
                    <section className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                        <StatCard icon={BankIcon} label="Linked accounts" value={String(accounts.length)} tint="blue" />
                        <StatCard icon={CheckmarkCircle02Icon} label="Active accounts" value={String(activeCount)} tint="success" />
                        <StatCard icon={Exchange02Icon} label="Currencies" value={String(totals.length)} tint="pink" />
                    </section>

                    <AccountOverview accounts={accounts} totals={totals} />
                </>
            )}

            <section className="flex flex-wrap gap-6 sm:gap-10">
                <QuickAction icon={BankIcon} label="Add a bank" href="/my-banks" />
                <QuickAction icon={SentIcon} label="Transfer funds" href="/payment-transfer" />
                <QuickAction icon={PieChart01Icon} label="Budgets" href="/budgets" />
                <QuickAction icon={UserIcon} label="Profile" href="/profile" />
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
