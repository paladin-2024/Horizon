/* eslint-disable no-unused-vars */

declare type SearchParamProps = {
  params: { [key: string]: string };
  searchParams: { [key: string]: string | string[] | undefined };
};

// ========================================

// The signed-in user is the API's GET /auth/me response (see api-auth.d.ts).
declare type User = ApiUser;

declare interface HeaderBoxProps {
  type?: "title" | "greeting";
  title: string;
  subtext: string;
  user?: string;
}

declare interface MobileNavProps {
  user: User;
}

declare interface SiderbarProps {
  user: User;
}

declare interface AuthFormProps {
  type: "sign-in" | "sign-up";
}

declare interface BankCardProps {
  account: ApiAccount;
  showBalance?: boolean;
}

declare interface TotalBalanceBoxProps {
  accounts: ApiAccount[];
  totals: Money[];
}

declare interface RightSidebarProps {
  user: User;
  accounts: ApiAccount[];
}
