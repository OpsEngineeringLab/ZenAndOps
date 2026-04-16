import { useEffect, useState } from "react";
import PageMeta from "../../components/common/PageMeta";
import ExecutiveSummaryCards from "../../components/dashboard/ExecutiveSummaryCards";
import TicketsByStateChart from "../../components/dashboard/TicketsByStateChart";
import SliSloComplianceCard from "../../components/dashboard/SliSloComplianceCard";
import IncidentMetricsCards from "../../components/dashboard/IncidentMetricsCards";
import ErrorBudgetCard from "../../components/dashboard/ErrorBudgetCard";
import ChangeFailureRateCard from "../../components/dashboard/ChangeFailureRateCard";
import apiClient from "../../api/ApiClient";
import type { DashboardPayload } from "../../types/dashboard";

export default function Home() {
  const [payload, setPayload] = useState<DashboardPayload | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function fetchDashboard() {
      try {
        setLoading(true);
        setError(null);
        const response = await apiClient.get<DashboardPayload>("/api/v1/dashboard");
        if (!cancelled) {
          setPayload(response.data);
        }
      } catch (err) {
        if (!cancelled) {
          setError("Failed to load dashboard data. Please try again later.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    fetchDashboard();

    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <>
        <PageMeta
          title="Dashboard | ZenAndOps"
          description="ZenAndOps operational dashboard"
        />
        <div className="flex items-center justify-center h-64">
          <div className="text-gray-500 dark:text-gray-400">Loading dashboard...</div>
        </div>
      </>
    );
  }

  if (error || !payload) {
    return (
      <>
        <PageMeta
          title="Dashboard | ZenAndOps"
          description="ZenAndOps operational dashboard"
        />
        <div className="flex items-center justify-center h-64">
          <div className="text-error-600 dark:text-error-400">
            {error || "No data available"}
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <PageMeta
        title="Dashboard | ZenAndOps"
        description="ZenAndOps operational dashboard"
      />
      <div className="grid grid-cols-12 gap-4 md:gap-6">
        {/* Executive Summary - full width */}
        <div className="col-span-12">
          <ExecutiveSummaryCards data={payload.executiveSummary} />
        </div>

        {/* Tickets by State bar chart */}
        <div className="col-span-12 xl:col-span-7">
          <TicketsByStateChart data={payload.ticketsByState} />
        </div>

        {/* SLI/SLO Compliance */}
        <div className="col-span-12 xl:col-span-5">
          <SliSloComplianceCard data={payload.sliSloCompliance} />
        </div>

        {/* MTTR and MTTD */}
        <div className="col-span-12">
          <IncidentMetricsCards data={payload.incidentMetrics} />
        </div>

        {/* Error Budget */}
        <div className="col-span-12 xl:col-span-6">
          <ErrorBudgetCard data={payload.errorBudget} />
        </div>

        {/* Change Failure Rate */}
        <div className="col-span-12 xl:col-span-6">
          <ChangeFailureRateCard data={payload.changeManagement} />
        </div>
      </div>
    </>
  );
}
