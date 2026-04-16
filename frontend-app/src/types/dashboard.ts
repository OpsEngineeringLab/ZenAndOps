/** Dashboard_Payload type matching the Dashboard_Service response */

export type TrendDirection = "UP" | "DOWN" | "STABLE";

export interface ExecutiveSummary {
  totalOpenTickets: number;
  criticalIncidents: number;
  overallAvailability: number;
  errorBudgetRemaining: number;
}

export interface TicketsByState {
  new: number;
  processingAssigned: number;
  processingPlanned: number;
  pending: number;
  solved: number;
  closed: number;
}

export interface SliSloCompliance {
  availabilitySli: number;
  availabilitySlo: number;
  latencySli: number;
  latencySlo: number;
}

export interface IncidentMetrics {
  mttrMinutes: number;
  mttrTrend: TrendDirection;
  mttdMinutes: number;
  mttdTrend: TrendDirection;
}

export interface ErrorBudget {
  remainingPercentage: number;
  burnRate: number;
  windowDays: number;
}

export interface ChangeManagement {
  changeFailureRatePercentage: number;
  totalChanges: number;
  failedChanges: number;
}

export interface DashboardPayload {
  executiveSummary: ExecutiveSummary;
  ticketsByState: TicketsByState;
  sliSloCompliance: SliSloCompliance;
  incidentMetrics: IncidentMetrics;
  errorBudget: ErrorBudget;
  changeManagement: ChangeManagement;
  errors: string[];
}
