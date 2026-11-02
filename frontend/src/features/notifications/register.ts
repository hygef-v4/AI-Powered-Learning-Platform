"use client";
import { headerWidgets } from "@/features/identity/slots";
import { NotificationBell } from "./components/NotificationBell";
headerWidgets.register({ key: "u14-notification-bell", order: 10, component: NotificationBell });
