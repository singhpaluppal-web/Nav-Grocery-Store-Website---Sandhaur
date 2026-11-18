import { NextRequest, NextResponse } from 'next/server';
import {
  INITIAL_VERIFIED_CATALOG,
  Product,
  ProductSchema,
  calculateWholesalePrice,
} from '@/lib/domain/catalog';

let catalogStore: Product[] = [...INITIAL_VERIFIED_CATALOG];

export async function GET() {
  return NextResponse.json({
    success: true,
    count: catalogStore.length,
    data: catalogStore,
  });
}

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const mrp = Number(body.mrp);
    const candidate = {
      ...body,
      id: body.id || `sku-${Date.now()}`,
      mrp,
      salePrice: calculateWholesalePrice(mrp),
      isFreshProduce: false as const,
      inStock: true,
    };

    const parsed = ProductSchema.safeParse(candidate);
    if (!parsed.success) {
      return NextResponse.json(
        { success: false, errors: parsed.error.flatten() },
        { status: 400 }
      );
    }

    catalogStore = [parsed.data, ...catalogStore.filter((p) => p.id !== parsed.data.id)];
    return NextResponse.json({ success: true, data: parsed.data }, { status: 201 });
  } catch {
    return NextResponse.json({ success: false, message: 'Invalid payload' }, { status: 400 });
  }
}

export async function DELETE(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const id = searchParams.get('id');
  if (!id) return NextResponse.json({ success: false }, { status: 400 });
  catalogStore = catalogStore.filter((p) => p.id !== id);
  return NextResponse.json({ success: true });
}
