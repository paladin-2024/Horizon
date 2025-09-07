import HeaderBox from '@/components/HeaderBox'
import RightSideBar from '@/components/RightSideBar';
import TotalBalanceBox from '@/components/TotalBalanceBox';

function Home() {
    const loggedIn= {firstName:'Caleb', lastName:'Nzabanita', email:'cnzabb@gmail.com'};
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
            RECENT TRANSACTION
        </div>
        <RightSideBar
            user={loggedIn}
            transactions={[]}
            banks={[{currentBalance:123.50}, {currentBalance:123.50}]}
        />
    </section>
    )
}

export default Home