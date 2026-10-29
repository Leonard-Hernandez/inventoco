/** API shapes (inventoco-api). Instants are ISO-8601 strings in UTC; quantities are in each item's unit. */

export type Dimension = 'COUNT' | 'MASS' | 'VOLUME';
export type MovementType = 'PURCHASE' | 'CONSUMPTION' | 'WASTE' | 'ADJUSTMENT';
export type Visibility = 'PRIVATE' | 'PUBLIC';
export type Confidence = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Profile {
  email: string | null;
  name: string | null;
  country: string;
  currency: string;
  timeZone: string;
  sharePricesByDefault: boolean;
  onboarded: boolean;
}

export interface UnitInfo { symbol: string; dimension: Dimension; factor: number; base: boolean; }

export interface Item {
  id: string;
  name: string;
  category: string | null;
  dimension: Dimension;
  unit: string;
  stock: number;
  minStock: number;
  lowStock: boolean;
  barcode: string | null;
  catalogProductId: string | null;
  updatedAt: string;
}

export interface ItemRequest {
  name: string;
  category?: string | null;
  unit: string;
  minStock?: number | null;
  initialStock?: number | null;
  barcode?: string | null;
  catalogProductId?: string | null;
}

export interface Movement {
  id: string;
  type: MovementType;
  quantity: number;
  unit: string;
  occurredAt: string;
  note: string | null;
  purchaseId: string | null;
}

export interface Price {
  id: string;
  supplierId: string;
  supplierName: string | null;
  amount: number;
  currency: string;
  packageQuantity: number;
  packageUnit: string;
  unitPrice: number;
  unitPriceUnit: string;
  observedAt: string;
  visibility: Visibility;
  purchaseId: string | null;
}

export interface PublicPrice {
  supplierName: string;
  city: string | null;
  country: string;
  amount: number;
  currency: string;
  packageQuantity: number;
  packageUnit: string;
  unitPrice: number;
  unitPriceUnit: string;
  observedAt: string;
  storeListing: boolean;
}

export interface Supplier { id: string; name: string; country: string; city: string | null; url: string | null; storeChain: boolean; }

export interface Forecast {
  itemId: string;
  name: string;
  unit: string;
  stock: number;
  minStock: number;
  lowStock: boolean;
  dailyUse: number;
  daysLeft: number | null;
  depletesAt: string | null;
  confidence: Confidence;
  samples: number;
  buySoon: boolean;
}

export interface PurchaseLine {
  itemId: string;
  itemName: string;
  unit: string;
  reason: 'LOW_STOCK' | 'RUNNING_OUT';
  suggestedQuantity: number;
  supplierId: string | null;
  supplierName: string | null;
  bestUnitPrice: number | null;
  usualUnitPrice: number | null;
  estimatedCost: number | null;
  estimatedSavings: number;
}

export interface Purchase {
  id: string;
  status: 'DRAFT' | 'COMPLETED';
  currency: string;
  horizonDays: number;
  createdAt: string;
  completedAt: string | null;
  lines: PurchaseLine[];
  estimatedTotal: number;
  estimatedSavings: number;
}

export interface BoughtLine {
  itemId: string;
  quantity: number;
  unit?: string | null;
  supplierId?: string | null;
  amount?: number | null;
  visibility?: Visibility | null;
}

export interface Savings {
  currency: string;
  days: number;
  realized: number;
  spent: number;
  purchases: number;
  estimatedOnShoppingList: number;
}

export interface SupplierRank { supplierId: string; name: string; city: string | null; priceIndex: number; prices: number; itemsCheapest: number; }

export interface Dashboard { buySoon: Forecast[]; savings: Savings; topSuppliers: SupplierRank[]; }

export interface CatalogProduct { id: string; name: string; brand: string | null; gtin: string | null; dimension: Dimension; }

export interface ApiError { code: string; message: string; }
