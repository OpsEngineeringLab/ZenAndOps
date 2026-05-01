<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html<#if realm.internationalizationEnabled> lang="${locale.currentLanguageTag}"</#if>>
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.png" />
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@100..900&display=swap" rel="stylesheet">
    <style>
        /* =============================================================================
         * ZenAndOps Keycloak Theme — Inline Styles
         * Replicates .frontend-template AuthPageLayout + SignInForm
         * ============================================================================= */

        :root {
            --brand-25: #f2f7ff;
            --brand-50: #ecf3ff;
            --brand-100: #dde9ff;
            --brand-200: #c2d6ff;
            --brand-300: #9cb9ff;
            --brand-400: #7592ff;
            --brand-500: #465fff;
            --brand-600: #3641f5;
            --brand-700: #2a31d8;
            --brand-800: #252dae;
            --brand-900: #262e89;
            --brand-950: #161950;
            --gray-25: #fcfcfd;
            --gray-50: #f9fafb;
            --gray-100: #f2f4f7;
            --gray-200: #e4e7ec;
            --gray-300: #d0d5dd;
            --gray-400: #98a2b3;
            --gray-500: #667085;
            --gray-600: #475467;
            --gray-700: #344054;
            --gray-800: #1d2939;
            --gray-900: #101828;
            --error-500: #f04438;
            --success-500: #12b76a;
            --shadow-xs: 0px 1px 2px 0px rgba(16, 24, 40, 0.05);
            --shadow-focus: 0px 0px 0px 4px rgba(70, 95, 255, 0.12);
        }

        *, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; }

        html, body {
            height: 100%;
            font-family: "Outfit", sans-serif;
            background: #ffffff;
            color: var(--gray-900);
            -webkit-font-smoothing: antialiased;
        }

        /* --- Split-Screen Container --- */
        .zenandops-auth-container {
            display: flex;
            min-height: 100vh;
            width: 100%;
        }

        /* --- Left Panel: Form --- */
        .zenandops-form-panel {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            width: 100%;
            padding: 2.5rem 1.5rem;
        }

        .zenandops-form-wrapper {
            width: 100%;
            max-width: 28rem;
        }

        /* --- Right Panel: Branded --- */
        .zenandops-brand-panel {
            display: none;
            width: 50%;
            min-height: 100vh;
            background-color: var(--brand-950);
            position: relative;
            overflow: hidden;
            align-items: center;
            justify-content: center;
        }

        .zenandops-brand-content {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            width: 100%;
            max-width: 20rem;
            position: relative;
            z-index: 1;
            text-align: center;
        }

        .zenandops-brand-logo {
            display: block;
            margin-bottom: 1rem;
        }

        .zenandops-brand-logo img {
            display: block;
            width: 231px;
            height: 48px;
        }

        .zenandops-brand-text {
            text-align: center;
            color: #98a2b3;
            font-size: 0.875rem;
            line-height: 1.5;
        }

        /* --- Grid Decorations --- */
        .zenandops-grid-top {
            position: absolute;
            top: 0;
            right: 0;
            z-index: 0;
            width: 100%;
            max-width: 450px;
            opacity: 0.6;
        }

        .zenandops-grid-bottom {
            position: absolute;
            bottom: 0;
            left: 0;
            z-index: 0;
            width: 100%;
            max-width: 450px;
            transform: rotate(180deg);
            opacity: 0.6;
        }

        /* --- Page Title --- */
        .zenandops-title {
            font-size: 1.875rem;
            font-weight: 600;
            color: var(--gray-800);
            line-height: 2.375rem;
            margin-bottom: 0.5rem;
        }

        .zenandops-subtitle {
            font-size: 0.875rem;
            color: var(--gray-500);
            margin-bottom: 2rem;
        }

        /* --- Form Groups --- */
        .zenandops-form-group {
            margin-bottom: 1.5rem;
        }

        .zenandops-label {
            display: block;
            font-size: 0.875rem;
            font-weight: 500;
            color: var(--gray-700);
            margin-bottom: 0.375rem;
        }

        .zenandops-required {
            color: var(--error-500);
        }

        .zenandops-input {
            width: 100%;
            height: 2.75rem;
            padding: 0.625rem 1rem;
            font-family: "Outfit", sans-serif;
            font-size: 0.875rem;
            color: var(--gray-800);
            background: transparent;
            border: 1px solid var(--gray-300);
            border-radius: 0.5rem;
            box-shadow: var(--shadow-xs);
            outline: none;
            transition: border-color 0.15s ease, box-shadow 0.15s ease;
        }

        .zenandops-input::placeholder {
            color: var(--gray-400);
        }

        .zenandops-input:focus {
            border-color: var(--brand-300);
            box-shadow: var(--shadow-focus);
        }

        .zenandops-input-error {
            display: block;
            margin-top: 0.375rem;
            font-size: 0.75rem;
            color: var(--error-500);
        }

        /* --- Checkbox --- */
        .zenandops-form-options {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 1.5rem;
        }

        .zenandops-checkbox-wrapper {
            display: flex;
            align-items: center;
            gap: 0.75rem;
        }

        .zenandops-checkbox {
            width: 1.25rem;
            height: 1.25rem;
            border: 1px solid var(--gray-300);
            border-radius: 0.375rem;
            cursor: pointer;
            accent-color: var(--brand-500);
        }

        .zenandops-checkbox-label {
            font-size: 0.875rem;
            font-weight: 400;
            color: var(--gray-700);
            cursor: pointer;
        }

        /* --- Primary Button --- */
        .zenandops-btn-primary {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 100%;
            padding: 0.75rem 1rem;
            font-family: "Outfit", sans-serif;
            font-size: 0.875rem;
            font-weight: 500;
            color: #ffffff;
            background-color: var(--brand-500);
            border: none;
            border-radius: 0.5rem;
            box-shadow: var(--shadow-xs);
            cursor: pointer;
            transition: background-color 0.15s ease;
            line-height: 1.5;
        }

        .zenandops-btn-primary:hover {
            background-color: var(--brand-600);
        }

        .zenandops-btn-primary:focus-visible {
            outline: none;
            box-shadow: var(--shadow-focus);
        }

        /* --- Form Buttons --- */
        .zenandops-form-buttons {
            margin-top: 0;
        }

        /* --- Alert Messages --- */
        .zenandops-alert {
            padding: 0.75rem 1rem;
            border-radius: 0.5rem;
            font-size: 0.875rem;
            margin-bottom: 1.25rem;
            line-height: 1.5;
        }

        .zenandops-alert-error {
            background-color: #fef3f2;
            border: 1px solid #fecdca;
            color: #b42318;
        }

        .zenandops-alert-warning {
            background-color: #fffaeb;
            border: 1px solid #fedf89;
            color: #b54708;
        }

        .zenandops-alert-success {
            background-color: #ecfdf3;
            border: 1px solid #a6f4c5;
            color: #027a48;
        }

        .zenandops-alert-info {
            background-color: var(--brand-50);
            border: 1px solid var(--brand-200);
            color: var(--brand-700);
        }

        /* --- Links --- */
        .zenandops-link {
            color: var(--brand-500);
            text-decoration: none;
            font-size: 0.875rem;
            transition: color 0.15s ease;
        }

        .zenandops-link:hover {
            color: var(--brand-600);
        }

        /* --- Info Section --- */
        .zenandops-info {
            margin-top: 1.25rem;
            text-align: center;
            font-size: 0.875rem;
            color: var(--gray-700);
        }

        .zenandops-info a {
            color: var(--brand-500);
            font-weight: 500;
            text-decoration: none;
        }

        .zenandops-info a:hover {
            color: var(--brand-600);
        }

        /* --- Locale Selector --- */
        .zenandops-locale {
            position: absolute;
            top: 1.5rem;
            right: 1.5rem;
            z-index: 10;
        }

        .zenandops-locale select {
            font-family: "Outfit", sans-serif;
            font-size: 0.875rem;
            color: var(--gray-600);
            background: #ffffff;
            border: 1px solid var(--gray-200);
            border-radius: 0.5rem;
            padding: 0.375rem 0.75rem;
            cursor: pointer;
            outline: none;
        }

        /* --- Responsive: show brand panel on large screens --- */
        @media (min-width: 1024px) {
            .zenandops-form-panel {
                width: 50%;
            }

            .zenandops-brand-panel {
                display: flex;
            }
        }

        @media (max-width: 640px) {
            .zenandops-title {
                font-size: 1.5rem;
                line-height: 2rem;
            }

            .zenandops-form-panel {
                padding: 1.5rem;
            }
        }

        /* --- Dark Mode --- */
        html.dark body,
        html.dark .zenandops-form-panel {
            background: #111827;
        }

        html.dark .zenandops-title {
            color: rgba(255, 255, 255, 0.9);
        }

        html.dark .zenandops-subtitle {
            color: var(--gray-400);
        }

        html.dark .zenandops-label {
            color: var(--gray-400);
        }

        html.dark .zenandops-input {
            background: #111827;
            color: rgba(255, 255, 255, 0.9);
            border-color: var(--gray-700);
        }

        html.dark .zenandops-input::placeholder {
            color: rgba(255, 255, 255, 0.3);
        }

        html.dark .zenandops-input:focus {
            border-color: var(--brand-800);
        }

        html.dark .zenandops-checkbox-label {
            color: var(--gray-400);
        }

        html.dark .zenandops-checkbox {
            border-color: var(--gray-700);
        }

        html.dark .zenandops-alert-error {
            background-color: rgba(240, 68, 56, 0.1);
            border-color: rgba(240, 68, 56, 0.3);
            color: #fda29b;
        }

        html.dark .zenandops-alert-info {
            background-color: rgba(70, 95, 255, 0.1);
            border-color: rgba(70, 95, 255, 0.3);
            color: var(--brand-300);
        }

        html.dark .zenandops-info {
            color: var(--gray-400);
        }

        html.dark .zenandops-brand-panel {
            background-color: rgba(255, 255, 255, 0.05);
        }

        html.dark .zenandops-brand-text {
            color: rgba(255, 255, 255, 0.6);
        }

        html.dark .zenandops-locale select {
            background: #111827;
            color: var(--gray-400);
            border-color: var(--gray-700);
        }

        /* --- Theme Toggle Button --- */
        .zenandops-theme-toggle {
            position: fixed;
            bottom: 1.5rem;
            right: 1.5rem;
            z-index: 50;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 3.5rem;
            height: 3.5rem;
            border-radius: 9999px;
            background-color: var(--brand-500);
            color: #ffffff;
            border: none;
            cursor: pointer;
            transition: background-color 0.15s ease;
            box-shadow: 0px 4px 8px -2px rgba(16, 24, 40, 0.1), 0px 2px 4px -2px rgba(16, 24, 40, 0.06);
        }

        .zenandops-theme-toggle:hover {
            background-color: var(--brand-600);
        }

        .zenandops-theme-toggle .icon-sun {
            display: none;
        }

        .zenandops-theme-toggle .icon-moon {
            display: block;
        }

        html.dark .zenandops-theme-toggle .icon-sun {
            display: block;
        }

        html.dark .zenandops-theme-toggle .icon-moon {
            display: none;
        }
    </style>
</head>
<body>
    <div class="zenandops-auth-container">
        <!-- Left Panel: Form -->
        <div class="zenandops-form-panel">
            <#if realm.internationalizationEnabled && locale.supported?size gt 1>
                <div class="zenandops-locale">
                    <select onchange="window.location.href=this.value">
                        <#list locale.supported as l>
                            <option value="${l.url}" <#if l.languageTag == locale.currentLanguageTag>selected</#if>>${l.label}</option>
                        </#list>
                    </select>
                </div>
            </#if>

            <div class="zenandops-form-wrapper">
                <div style="margin-bottom: 1.5rem;">
                    <h1 class="zenandops-title"><#nested "header"></h1>
                </div>

                <#-- Alert Messages -->
                <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                    <div class="zenandops-alert zenandops-alert-${message.type}">
                        ${kcSanitize(message.summary)?no_esc}
                    </div>
                </#if>

                <#nested "form">

                <#if displayInfo>
                    <div class="zenandops-info">
                        <#nested "info">
                    </div>
                </#if>
            </div>
        </div>

        <!-- Right Panel: Brand -->
        <div class="zenandops-brand-panel">
            <!-- Grid decoration top-right -->
            <svg class="zenandops-grid-top" viewBox="0 0 450 254" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path fill-rule="evenodd" clip-rule="evenodd" d="M0.5 45.1h450v-.5H0.5v.5z" fill="url(#g1)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M205.5 253.5V0h-.5v253.5h.5z" fill="url(#g2)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M0.5 97.2h450v-.5H0.5v.5z" fill="url(#g3)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M256.8 253.5V0h-.5v253.5h.5z" fill="url(#g4)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M0.5 253.5V0H0v253.5h.5z" fill="url(#g5)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M0.5 149.3h450v-.5H0.5v.5z" fill="url(#g6)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M308 253.5V0h-.5v253.5h.5z" fill="url(#g7)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M51.8 253.5V0h-.5v253.5h.5z" fill="url(#g8)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M0.5 201.4h450v-.5H0.5v.5z" fill="url(#g9)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M359.3 253.5V0h-.5v253.5h.5z" fill="url(#g10)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M103 253.5V0h-.5v253.5h.5z" fill="url(#g11)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M410.6 253.5V0h-.5v253.5h.5z" fill="url(#g12)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M154.3 253.5V0h-.5v253.5h.5z" fill="url(#g13)" fill-opacity="0.3"/>
                <rect width="50.75" height="51.6" transform="matrix(-1 0 0 1 358.8 45.1)" fill="#B2B2B2" fill-opacity="0.08"/>
                <rect width="50.75" height="51.6" transform="matrix(-1 0 0 1 307.6 97.2)" fill="#B2B2B2" fill-opacity="0.08"/>
                <defs>
                    <linearGradient id="g1" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="g2" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="g3" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="g4" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="g5" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="g6" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="g7" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="g8" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="g9" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="g10" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="g11" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="g12" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="g13" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                </defs>
            </svg>

            <!-- Grid decoration bottom-left (rotated 180deg) -->
            <svg class="zenandops-grid-bottom" viewBox="0 0 450 254" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path fill-rule="evenodd" clip-rule="evenodd" d="M0.5 45.1h450v-.5H0.5v.5z" fill="url(#gb1)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M205.5 253.5V0h-.5v253.5h.5z" fill="url(#gb2)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M0.5 97.2h450v-.5H0.5v.5z" fill="url(#gb3)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M256.8 253.5V0h-.5v253.5h.5z" fill="url(#gb4)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M0.5 149.3h450v-.5H0.5v.5z" fill="url(#gb5)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M308 253.5V0h-.5v253.5h.5z" fill="url(#gb6)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M51.8 253.5V0h-.5v253.5h.5z" fill="url(#gb7)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M0.5 201.4h450v-.5H0.5v.5z" fill="url(#gb8)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M359.3 253.5V0h-.5v253.5h.5z" fill="url(#gb9)" fill-opacity="0.3"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M103 253.5V0h-.5v253.5h.5z" fill="url(#gb10)" fill-opacity="0.3"/>
                <rect width="50.75" height="51.6" transform="matrix(-1 0 0 1 358.8 45.1)" fill="#B2B2B2" fill-opacity="0.08"/>
                <rect width="50.75" height="51.6" transform="matrix(-1 0 0 1 307.6 97.2)" fill="#B2B2B2" fill-opacity="0.08"/>
                <defs>
                    <linearGradient id="gb1" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="gb2" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="gb3" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="gb4" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="gb5" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="gb6" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="gb7" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="gb8" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="gb9" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                    <linearGradient id="gb10" x1="278" y1="0" x2="195" y2="236" gradientUnits="userSpaceOnUse"><stop stop-color="#B2B2B2"/><stop offset="1" stop-color="#B2B2B2" stop-opacity="0"/></linearGradient>
                </defs>
            </svg>

            <div class="zenandops-brand-content">
                <div class="zenandops-brand-logo">
                    <img src="${url.resourcesPath}/img/auth-logo.svg" alt="ZenAndOps" width="231" height="48" />
                </div>
                <p class="zenandops-brand-text">
                    Unified IT Service Management &amp; Site Reliability Platform
                </p>
            </div>
        </div>
    </div>

    <!-- Theme Toggle Button -->
    <button class="zenandops-theme-toggle" onclick="toggleTheme()" aria-label="Toggle dark mode">
        <!-- Sun icon (shown in dark mode) -->
        <svg class="icon-sun" width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path fill-rule="evenodd" clip-rule="evenodd" d="M10 1.54c.414 0 .75.336.75.75v1.25a.75.75 0 01-1.5 0V2.29a.75.75 0 01.75-.75zm0 5.253a3.207 3.207 0 100 6.414 3.207 3.207 0 000-6.414zM5.294 10a4.707 4.707 0 119.414 0 4.707 4.707 0 01-9.414 0zm10.687-5.92a.75.75 0 00-1.06-1.06l-.884.884a.75.75 0 001.06 1.06l.884-.884zM18.458 10a.75.75 0 01-.75.75h-1.25a.75.75 0 010-1.5h1.25a.75.75 0 01.75.75zm-3.537 5.98a.75.75 0 001.06-1.06l-.884-.884a.75.75 0 00-1.06 1.06l.884.884zM10 15.709a.75.75 0 01.75.75v1.25a.75.75 0 01-1.5 0v-1.25a.75.75 0 01.75-.75zm-4.036-.612a.75.75 0 00-1.06-1.061l-.885.884a.75.75 0 001.06 1.06l.885-.883zM4.292 10a.75.75 0 01-.75.75H2.292a.75.75 0 010-1.5h1.25a.75.75 0 01.75.75zm.611-4.036a.75.75 0 001.06-1.061L5.08 4.02a.75.75 0 00-1.06 1.06l.883.884z" fill="currentColor"/>
        </svg>
        <!-- Moon icon (shown in light mode) -->
        <svg class="icon-moon" width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M17.455 11.97l.725.191a.75.75 0 00-1.236-.74l.511.549zM8.031 2.546l.549.51a.75.75 0 00-.74-1.236l.191.726zM12.915 13.004a5.919 5.919 0 01-5.918-5.919h-1.5c0 4.097 3.321 7.419 7.418 7.419v-1.5zm4.029-1.583A6.958 6.958 0 0110 16.959v1.5c3.926 0 7.225-2.673 8.18-6.297l-1.45-.382zM10 16.959a6.958 6.958 0 01-6.958-6.959h-1.5c0 4.672 3.787 8.459 8.458 8.459v-1.5zm-6.958-6.959a6.958 6.958 0 015.18-6.729l-.383-1.45C4.215 2.776 1.542 6.075 1.542 10h1.5zm2.955-2.915c0-1.557.6-2.972 1.583-4.029l-1.099-1.021A7.419 7.419 0 005.497 7.085h1.5z" fill="currentColor"/>
        </svg>
    </button>

    <script>
        (function() {
            var theme = localStorage.getItem('zenandops-theme');
            if (theme === 'dark' || (!theme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
                document.documentElement.classList.add('dark');
            }
        })();

        function toggleTheme() {
            var html = document.documentElement;
            if (html.classList.contains('dark')) {
                html.classList.remove('dark');
                localStorage.setItem('zenandops-theme', 'light');
            } else {
                html.classList.add('dark');
                localStorage.setItem('zenandops-theme', 'dark');
            }
        }
    </script>
</body>
</html>
</#macro>
