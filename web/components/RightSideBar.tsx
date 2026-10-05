import Link from "next/link";
import BankCard from "./BankCard";
import Icon from "./Icon";
import PlusSignIcon from "@hugeicons/core-free-icons/PlusSignIcon";

const RightSideBar = ({ user, accounts }: RightSidebarProps) => {
  const preview = accounts.slice(0, 2);

  return (
    <div>
      <aside className="right-sidebar">
        <section className="flex flex-col pb-8">
          <div className="profile-banner" />
          <div className="profile">
            <div className="profile-img">
              <span className="text-5xl font-bold text-blue-500">
                {user.firstName[0]}
              </span>
            </div>
            <div className="profile-details">
              <h1 className="profile-name">
                {user.firstName} {user.lastName}
              </h1>
              <p className="profile-email">{user.email ?? user.phone}</p>
            </div>
          </div>
        </section>
        <section className="banks">
          <div className="flex w-full justify-between">
            <h2 className="header-2">My Banks</h2>
            <Link href="/my-banks" className="flex gap-2">
              <Icon icon={PlusSignIcon} size={18} className="text-gray-600" />
              <h2 className="text-14 font-semibold text-gray-600">Add Bank</h2>
            </Link>
          </div>
          {preview.length > 0 ? (
            <div className="relative flex flex-1 flex-col items-center justify-center gap-5">
              <div className="relative z-10">
                <BankCard account={preview[0]} showBalance={false} />
              </div>
              {preview[1] && (
                <div className="absolute right-0 top-8 z-0 w-[90%]">
                  <BankCard account={preview[1]} showBalance={false} />
                </div>
              )}
            </div>
          ) : (
            <p className="text-14 text-gray-500">
              No accounts linked yet — add one from My Banks.
            </p>
          )}
        </section>
      </aside>
    </div>
  );
};

export default RightSideBar;
