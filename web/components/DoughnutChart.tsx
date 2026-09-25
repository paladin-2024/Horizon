"use client"
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from "chart.js";
import { Doughnut } from "react-chartjs-2";
ChartJS.register(ArcElement, Tooltip, Legend);



const PALETTE = ["#0179FE", "#2265d8", "#2f91fa", "#4893FF", "#194185"];

const DoughnutChart = ({accounts}:DoughnutChartProps) => {
    const hasAccounts = accounts.length > 0;
    const values = hasAccounts ? accounts.map((a) => a.currentBalance) : [1];
    const labels = hasAccounts ? accounts.map((a) => a.name) : ["Balance"];

    const data={
        datasets: [
            {
                label:"Banks",
                data: values,
                backgroundColor: PALETTE.slice(0, values.length)
            }
        ],
        labels
    }

    return    <Doughnut 
    data={data} 
    options={{
        cutout:'60%',
        plugins:{
            legend:{
                display: false,
            }
        }
    }}
    />
}

export default DoughnutChart
