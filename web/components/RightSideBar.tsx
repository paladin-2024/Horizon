import Link from "next/link";
import BankCard from "./BankCard";
import Icon from "./Icon";
import { Avatar, AvatarFallback } from "./ui/avatar";
import { Progress } from "./ui/progress";
import PlusSignIcon from "@hugeicons/core-free-icons/PlusSignIcon";

const RightSideBar = ({ user, accounts }: RightSidebarProps) => {
  const preview = accounts.slice(0, 2);
  const bankCount = accounts.filter((a) => a.institution?.type !== "MOBILE_MONEY").length;
  const mobileMoneyCount = accounts.length - bankCount;
  const activeCount = accounts.filter((a) => a.status === "ACTIVE").length;
  const archivedCount = accounts.length - activeCount;
  const bankPct = accounts.length > 0 ? Math.round((bankCount / accounts.length) * 100) : 0;

  return (
    <div>
      <aside className="right-sidebar">
        <section className="flex flex-col pb-8">
          <div className="profile-banner" />
          <div className="profile">
            <div className="profile-img">
              <Avatar className="h-full w-full">
                <AvatarFallback className="bg-transparent text-5xl font-bold text-blue-500">
                  {user.firstName[0]}
                </AvatarFallback>
              </Avatar>
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

          {accounts.length > 0 && (
            <div className="flex flex-col gap-3 rounded-lg border border-gray-100 p-3">
              <p className="text-12 text-gray-500">By provider</p>

              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between text-14">
                  <span className="flex items-center gap-2 text-gray-700">
                    <span className="size-2 rounded-full bg-blue-600" />
                    Banks
                  </span>
                  <span className="font-semibold text-gray-900">{bankCount}</span>
                </div>
                <div className="flex items-center justify-between text-14">
                  <span className="flex items-center gap-2 text-gray-700">
                    <span className="size-2 rounded-full bg-indigo-500" />
                    Mobile money
                  </span>
                  <span className="font-semibold text-gray-900">{mobileMoneyCount}</span>
                </div>
              </div>

              <Progress value={bankPct} className="h-1.5 bg-indigo-500" indicatorClassName="bg-blue-600" />

              <div className="grid grid-cols-2 gap-2">
                <div className="rounded-lg bg-success-25 p-2.5">
                  <p className="text-12 text-success-700">Active</p>
                  <p className="text-16 font-semibold text-success-900">{activeCount}</p>
                </div>
                <div className="rounded-lg bg-gray-100 p-2.5">
                  <p className="text-12 text-gray-500">Archived</p>
                  <p className="text-16 font-semibold text-gray-700">{archivedCount}</p>
                </div>
              </div>
            </div>
          )}

          {preview.length > 0 ? (
            <div className="flex flex-col items-center gap-4">
              {preview.map((account) => (
                <BankCard key={account.id} account={account} showBalance={false} />
              ))}
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
