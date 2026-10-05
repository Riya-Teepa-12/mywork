import { act, fireEvent, render, screen } from "@testing-library/react";
import {
  NotificationProvider,
  notifyError,
  notifyInfo,
  notifySuccess,
  useNotification,
} from "../context/NotificationContext.jsx";

function Probe() {
  const { success } = useNotification();
  return <button onClick={() => success("Saved!")}>trigger</button>;
}

describe("NotificationContext", () => {
  it("shows notifications from helper dispatchers", () => {
    render(
      <NotificationProvider>
        <Probe />
      </NotificationProvider>
    );

    act(() => notifySuccess("Great"));
    expect(screen.getByText("Great")).toBeInTheDocument();

    act(() => notifyError("Oops"));
    expect(screen.getByText("Oops")).toBeInTheDocument();

    act(() => notifyInfo("FYI"));
    expect(screen.getByText("FYI")).toBeInTheDocument();

    fireEvent.click(screen.getByText("trigger"));
    expect(screen.getByText("Saved!")).toBeInTheDocument();

    fireEvent.click(screen.getAllByLabelText("Dismiss notification")[0]);
  });

  it("dedupes identical notifications in short window", () => {
    render(
      <NotificationProvider>
        <Probe />
      </NotificationProvider>
    );

    act(() => notifyInfo("Same message"));
    act(() => notifyInfo("Same message"));
    expect(screen.getAllByText("Same message").length).toBe(1);

    act(() => notifyInfo("   "));
    expect(screen.queryByText("   ")).not.toBeInTheDocument();
  });
});
