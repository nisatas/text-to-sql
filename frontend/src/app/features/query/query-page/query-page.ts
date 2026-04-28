import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Query, QueryResponse } from '../../../core/services/query';
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
export class QueryPage {
  question: string = '';
  response: QueryResponse | null = null;
  loading = false;
  tableData: Record<string, any>[] = [];
  displayedColumns: string[] = [];
  errorMessage = ' ';

  constructor(private queryService: Query) {}

  onSubmit() {
    if (!this.question.trim()) {
      this.errorMessage = 'Lütfen bir soru yazın.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.response = null;
    this.tableData =[];
    this.displayedColumns= [];

    this.queryService.sendQuery(this.question).subscribe({
      next: (res) => {
        console.log('Backend cevabı:', res);

  this.response = res;
  this.loading = false;

  if (res.status === 'success') {
    this.displayedColumns = res.columns ?? [];
    this.tableData = this.mapRowsToObjects(res.columns, res.rows);
  }

  if (res.status === 'error') {
    this.errorMessage = res.error ?? 'Bir hata oluştu.';
    
  }
      },
      error: () => {
        this.errorMessage = 'Sunucuya ulaşılamadı.';
        this.loading = false;
        this.tableData = [];
      this.displayedColumns = [];
      },
    });
  }
  mapRowsToObjects(columns: string[] | null, rows: any[][] | null): Record<string, any>[] {
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

  // Başlık
  doc.text('Öğrenci Sorgu Sonuçları', 14, 15);

  // Tablo kolonları
  const columns = this.displayedColumns;

  // Satırları object -> array çeviriyoruz
  const rows = this.tableData.map((row) =>
    columns.map((col) => row[col])
  );

  // Tabloyu PDF'e bas
  autoTable(doc, {
    head: [columns],
    body: rows,
    startY: 20,
  });

  // Kaydet
  doc.save('sorgu-sonuclari.pdf');
}
downloadExcel() {
  const worksheet = XLSX.utils.json_to_sheet(this.tableData);
  const workbook = XLSX.utils.book_new();

  XLSX.utils.book_append_sheet(workbook, worksheet, 'Sonuçlar');

  XLSX.writeFile(workbook, 'sorgu-sonuclari.xlsx');
}

}
