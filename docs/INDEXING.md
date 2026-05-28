**INNERVEX TECHNOLOGIES PRIVATE LIMITED**

# INNERVEX® DATACORE

### Enterprise Data Platform & Integration Framework

**Version:** 1.0  

**Built on Open Standards. Engineered for Enterprise Operations.**

---

| INNERVEX TECHNOLOGIES PRIVATE LIMITED | INNERVEX® DATACORE<br>Enterprise Data Platform & Integration Framework | Version: 1.0<br>Document Type: Indexing Guidance & Technical Overview |
| --- | --- | --- |

## 1. Technology Notice

Innervex DataCore is an enterprise data platform, deployment framework,
integration architecture, and support ecosystem developed by Innervex
Technologies.

Innervex DataCore may incorporate and utilise open-source technologies and
database engines as part of its implementation architecture. All underlying
open-source software components remain the property of their respective
copyright holders and are governed by their respective licences.

Innervex Technologies does not claim ownership of any third-party open-source
projects included within the Innervex DataCore ecosystem.

# 2. Indexing Notes

**INNERVEX® DATACORE**  
Enterprise Data Platform & Integration Framework  
Built on Open Standards. Engineered for Enterprise Operations.

These notes capture practical indexing rules proven by the local DataCore
benchmarks and Derby regression tests.

## Covering Range Scans

For ordered range queries, prefer an index that covers both:

- the range and ordering columns
- the columns returned by the query

For example, this query filters and orders by `amount, id`, but also returns
`name`:

```sql
select id, amount, name
from baseline_item
where amount between ? and ?
order by amount, id
```

The ordered index below can position rows efficiently, but Derby still needs to
fetch the base table row to read `name`:

```sql
create index baseline_item_amount_id_idx
on baseline_item(amount, id)
```

The covering index below lets Derby answer the query from the index itself:

```sql
create index baseline_item_amount_id_name_idx
on baseline_item(amount, id, name)
```

When both indexes exist, Derby's optimizer chooses the covering index for this
query shape. That behavior is protected by
`CoveringIndexOptimizerTest`.

## Tradeoff

Covering indexes speed up read paths when they avoid base-table row fetches, but
they add write cost because inserts, updates, and deletes must maintain the
larger index. Use the covering index when the range query is important enough to
justify that write cost.

## Current Evidence

The benchmark suite shows the full-row covering range scan running much faster
than the non-covering range scan on the current baseline:

- `range-scan`: base-table fetch required
- `range-scan-full-covering-index`: index contains `amount, id, name`
- `range-scan-dual-index`: both indexes exist; Derby chooses the covering index

Run the suite with:

```sh
bin/datacore-benchmark-suite
```

---

**Innervex® DataCore Documentation**
