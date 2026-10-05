import Image from "next/image";
import Icon from "./Icon";
import Wifi02Icon from "@hugeicons/core-free-icons/Wifi02Icon";
import { formatMoney } from "@/lib/money";

const BankCard = ({ account, showBalance = true }: BankCardProps) => {
  return (
    <div className="flex flex-col">
      <div className="bank-card">
        <div className="bank-card_content">
          <div>
            <h1 className="text-16 font-semibold text-white">{account.displayName}</h1>
            {showBalance && (
              <p className="font-sans text-20 font-bold text-white">
                {formatMoney(account.balance.amountMinor, account.balance.currency)}
              </p>
            )}
          </div>

          <article className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <h2 className="text-12 font-semibold uppercase tracking-wide text-white/80">
                {account.institution?.name ?? "Linked account"}
              </h2>
              <Icon icon={Wifi02Icon} size={18} className="rotate-90 text-white/80" />
            </div>
            <p className="text-14 font-semibold tracking-[1.1px] text-white">
              •••• •••• •••• <span className="text-16">{account.accountMask}</span>
            </p>
          </article>
        </div>

        <div className="bank-card_icon">
          <span className="rounded-full border border-white/40 px-2 py-0.5 text-10 font-semibold uppercase tracking-wide text-white/90">
            {account.balance.currency}
          </span>
        </div>
        <Image
          src="/icons/lines.png"
          width={316}
          height={190}
          alt=""
          className="pointer-events-none absolute left-0 top-0 opacity-60"
        />
      </div>
    </div>
  );
};

export default BankCard;
