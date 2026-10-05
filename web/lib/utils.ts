import { type ClassValue, clsx } from "clsx";
import qs from "query-string";
import { twMerge } from "tailwind-merge";
import { z } from "zod";
import { isValidPhone } from "@/lib/phone";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// FORMAT DATE TIME
export const formatDateTime = (dateString: Date) => {
  const dateTimeOptions: Intl.DateTimeFormatOptions = {
    weekday: "short", // abbreviated weekday name (e.g., 'Mon')
    month: "short", // abbreviated month name (e.g., 'Oct')
    day: "numeric", // numeric day of the month (e.g., '25')
    hour: "numeric", // numeric hour (e.g., '8')
    minute: "numeric", // numeric minute (e.g., '30')
    hour12: true, // use 12-hour clock (true) or 24-hour clock (false)
  };

  const dateDayOptions: Intl.DateTimeFormatOptions = {
    weekday: "short", // abbreviated weekday name (e.g., 'Mon')
    year: "numeric", // numeric year (e.g., '2023')
    month: "2-digit", // abbreviated month name (e.g., 'Oct')
    day: "2-digit", // numeric day of the month (e.g., '25')
  };

  const dateOptions: Intl.DateTimeFormatOptions = {
    month: "short", // abbreviated month name (e.g., 'Oct')
    year: "numeric", // numeric year (e.g., '2023')
    day: "numeric", // numeric day of the month (e.g., '25')
  };

  const timeOptions: Intl.DateTimeFormatOptions = {
    hour: "numeric", // numeric hour (e.g., '8')
    minute: "numeric", // numeric minute (e.g., '30')
    hour12: true, // use 12-hour clock (true) or 24-hour clock (false)
  };

  const formattedDateTime: string = new Date(dateString).toLocaleString(
    "en-US",
    dateTimeOptions
  );

  const formattedDateDay: string = new Date(dateString).toLocaleString(
    "en-US",
    dateDayOptions
  );

  const formattedDate: string = new Date(dateString).toLocaleString(
    "en-US",
    dateOptions
  );

  const formattedTime: string = new Date(dateString).toLocaleString(
    "en-US",
    timeOptions
  );

  return {
    dateTime: formattedDateTime,
    dateDay: formattedDateDay,
    dateOnly: formattedDate,
    timeOnly: formattedTime,
  };
};

export function formatAmount(amount: number): string {
  const formatter = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
  });

  return formatter.format(amount);
}

interface UrlQueryParams {
  params: string;
  key: string;
  value: string;
}

export function formUrlQuery({ params, key, value }: UrlQueryParams) {
  const currentUrl = qs.parse(params);

  currentUrl[key] = value;

  return qs.stringifyUrl(
    {
      url: window.location.pathname,
      query: currentUrl,
    },
    { skipNull: true }
  );
}

export function encryptId(id: string) {
  return btoa(id);
}

export function decryptId(id: string) {
  return atob(id);
}

const PHONE_MESSAGE = "Enter a valid phone number, for example +256771234567";

export const authFormSchema = (type:string)  => z.object({
  //sih Up
  firstName:type === "sign-in" ? z.string().optional() : z.string().min(3),
  lastName: type === "sign-in" ? z.string().optional() :z.string().min(3),
  address1: type === "sign-in" ? z.string().optional() :z.string().max(50),
  city: type === "sign-in" ? z.string().optional() :z.string().min(3).max(15),
  state: type === "sign-in" ? z.string().optional() :z.string().min(3).max(15),
  postalCode: type === "sign-in" ? z.string().optional() :z.string().min(3).max(6),
  dateOfBirth: type === "sign-in" ? z.string().optional() :z.string().min(3),
  nationalId: type === "sign-in" ? z.string().optional() :z.string().min(3),
  phone:
    type === "sign-in"
      ? z.string().optional()
      : z.string().refine(isValidPhone, { message: PHONE_MESSAGE }),
  //both
  email: z.string().email(),
  password: z.string().min(6),
})

export const otpFormSchema = z.object({
  phone: z.string().refine(isValidPhone, { message: PHONE_MESSAGE }),
  code: z.string().regex(/^\d{6}$/, "Enter the 6-digit code"),
});

// Mirrors AccountDtos.CreateAccountRequest's validation (backend/.../AccountDtos.java).
export const linkAccountFormSchema = z.object({
  institutionId: z.string().uuid("Choose an institution"),
  provider: z.literal("MANUAL"),
  displayName: z.string().trim().min(1, "Give this account a name").max(80),
  accountMask: z.string().regex(/^\d{4}$/, "Enter the last 4 digits"),
  currency: z.enum(["UGX", "CDF", "USD"], { required_error: "Choose a currency" }),
  openingBalance: z
    .string()
    .min(1, "Enter the current balance")
    .refine((v) => !Number.isNaN(Number(v)) && Number(v) >= 0, "Enter a valid amount"),
});