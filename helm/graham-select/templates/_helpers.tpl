{{/*
Expand the name of the chart.
*/}}
{{- define "graham-select.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "graham-select.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "graham-select.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{ include "graham-select.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "graham-select.selectorLabels" -}}
app.kubernetes.io/name: {{ include "graham-select.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Backend API names & labels
*/}}
{{- define "graham-select.api.fullname" -}}
{{- printf "%s-api" (include "graham-select.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "graham-select.api.selectorLabels" -}}
app.kubernetes.io/name: {{ include "graham-select.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: api
{{- end }}

{{/*
Backend Valuation names & labels
*/}}
{{- define "graham-select.valuation.fullname" -}}
{{- printf "%s-valuation" (include "graham-select.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "graham-select.valuation.selectorLabels" -}}
app.kubernetes.io/name: {{ include "graham-select.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: valuation
{{- end }}

{{/*
Frontend names & labels
*/}}
{{- define "graham-select.frontend.fullname" -}}
{{- printf "%s-frontend" (include "graham-select.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "graham-select.frontend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "graham-select.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: frontend
{{- end }}
