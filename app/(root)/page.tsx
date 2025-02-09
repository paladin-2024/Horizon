import HeaderBox from '@/components/HeaderBox'
import TotalBalanceBox from '@/components/TotalBalanceBox';

function Home() {
    const logginIn= {firstName:'Paladin'};
    return (
    <section className='home'>
        <div className='home-content'>
            <header className='home-header'>
                <HeaderBox
                    type="greeting"
                    title="Welcome"
                    user={logginIn?.firstName || 'Guest'}
                    subtext="Access and manage your account and transactions efficiently."
                />

                <TotalBalanceBox 
                accounts={[]}
                totalBanks={1}
                totalCurrentBalance={1250.35}
                />
            </header>
        </div>
    </section>
    )
}

export default Home