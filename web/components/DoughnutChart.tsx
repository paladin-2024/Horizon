"use client"
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from "chart.js";
import { Doughnut } from "react-chartjs-2";
import { formatMoney } from "@/lib/money";
ChartJS.register(ArcElement, Tooltip, Legend);

const PALETTE = ["#0179FE", "#2265d8", "#2f91fa", "#4893FF", "#194185", "#6172F3"];

/** Renders accounts that already share one currency — callers must group by currency first. */
const DoughnutChart = ({ accounts }: { accounts: ApiAccount[] }) => {
    const hasAccounts = accounts.length > 0;
    const currency = accounts[0]?.balance.currency ?? "";

    const data = hasAccounts
        ? {
              datasets: [
                  {
                      label: "Accounts",
                      data: accounts.map((a) => a.balance.amountMinor),
                      backgroundColor: PALETTE.slice(0, accounts.length),
                      borderWidth: 0,
                  },
              ],
              labels: accounts.map((a) => a.displayName),
          }
        : {
              // A single flat gray ring reads as "no data," not "100% of something."
              datasets: [{ label: "Accounts", data: [1], backgroundColor: ["#EAECF0"], borderWidth: 0 }],
              labels: ["No accounts linked"],
          };

    return (
        <Doughnut
            data={data}
            options={{
                cutout: "68%",
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        enabled: hasAccounts,
                        backgroundColor: "#101828",
                        padding: 10,
                        cornerRadius: 8,
                        titleFont: { family: "var(--font-sans)", weight: 600 },
                        bodyFont: { family: "var(--font-sans)" },
                        callbacks: {
                            label: (ctx) => formatMoney(ctx.parsed, currency),
                        },
                    },
                },
            }}
        />
    );
};

export default DoughnutChart;
