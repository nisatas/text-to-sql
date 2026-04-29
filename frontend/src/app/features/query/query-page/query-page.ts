import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FilterQueryRequest, Query, QueryResponse } from '../../../core/services/query';
import { NgFor, NgIf } from '@angular/common';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import * as XLSX from 'xlsx';

@Component({
  selector: 'app-query-page',
  imports: [FormsModule, NgFor, NgIf],
  templateUrl: './query-page.html',
  styleUrl: './query-page.css',
})
export class QueryPage implements OnInit {
  question = '';
  response: QueryResponse | null = null;
  loading = false;
  tableData: Record<string, any>[] = [];
  displayedColumns: string[] = [];
  errorMessage = '';
  includeSummary = false;
  mode: 'nlp' | 'filters' = 'filters';

  readonly subjects = ['Matematik', 'Fizik', 'Kimya', 'Türkçe', 'Biyoloji'] as const;

  filters: FilterQueryRequest = {
    outputMode: 'records',
    className: null,
    gradeLevel: null,
    studentNumber: null,
    studentName: null,
    subject: null,
    examNo: null,
    minScore: null,
    maxScore: null,
  };

  constructor(private queryService: Query) {}

  ngOnInit() {}

  onSubmit() {
    if (this.mode === 'filters') {
      this.onSubmitFilters();
      return;
    }
    if (!this.question.trim()) {
      this.errorMessage = 'Lütfen bir soru yazın.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.response = null;
    this.tableData = [];
    this.displayedColumns = [];

    this.queryService.sendQuery(this.question, this.includeSummary).subscribe({
      next: (res) => {
        this.response = res;
        this.loading = false;

        this.applyResponse(res);
      },
      error: () => {
        this.errorMessage = 'Sunucuya ulaşılamadı.';
        this.loading = false;
        this.tableData = [];
        this.displayedColumns = [];
      },
    });
  }

  onSubmitFilters() {
    this.loading = true;
    this.errorMessage = '';
    this.response = null;
    this.tableData = [];
    this.displayedColumns = [];

    const payload: FilterQueryRequest = {
      ...this.filters,
      className: this.filters.className?.trim() || null,
      studentNumber: this.filters.studentNumber?.trim() || null,
      studentName: this.filters.studentName?.trim() || null,
      subject: this.filters.subject?.trim() || null,
    };

    this.queryService.sendFilterQuery(payload).subscribe({
      next: (res) => {
        this.response = res;
        this.loading = false;
        this.applyResponse(res);
      },
      error: () => {
        this.errorMessage = 'Sunucuya ulaşılamadı.';
        this.loading = false;
      },
    });
  }

  resetFilters() {
    this.filters = {
      outputMode: 'records',
      className: null,
      gradeLevel: null,
      studentNumber: null,
      studentName: null,
      subject: null,
      examNo: null,
      minScore: null,
      maxScore: null,
    };
  }

  private applyResponse(res: QueryResponse) {
    if (res.status === 'success') {
      this.displayedColumns = res.columns ?? [];
      this.tableData = this.mapRowsToObjects(res.columns, res.rows);
      return;
    }
    this.errorMessage = res.error ?? 'Bir hata oluştu.';
  }

  private mapRowsToObjects(columns: string[] | null, rows: any[][] | null): Record<string, any>[] {
    if (!columns || !rows) return [];

    return rows.map((row) =>
      Object.fromEntries(columns.map((column, index) => [column, row[index]])),
    );
  }
  
  setExample(text: string) {
    this.question = text;
  }

  downloadPdf() {
    const doc = new jsPDF();

    doc.text('Öğrenci Sorgu Sonuçları', 14, 15);

    const columns = this.displayedColumns;

    const rows = this.tableData.map((row) => columns.map((col) => row[col]));

    autoTable(doc, {
      head: [columns],
      body: rows,
      startY: 20,
    });

    doc.save('sorgu-sonuclari.pdf');
  }

  downloadExcel() {
    const worksheet = XLSX.utils.json_to_sheet(this.tableData);
    const workbook = XLSX.utils.book_new();

    XLSX.utils.book_append_sheet(workbook, worksheet, 'Sonuçlar');
    XLSX.writeFile(workbook, 'sorgu-sonuclari.xlsx');
  }

}
