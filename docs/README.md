# Documentation Map

This directory is the entry point for product, business, architecture, data,
delivery, and engineering context.

## Reading order

For a non-trivial change, read only the relevant documents, in this order:

1. [Product documentation](product/README.md)
2. [Business Decision Records](bdr/README.md)
3. [Architecture Decision Records](adr/README.md)
4. [Engineering standards](engineering/README.md)
5. [Logical data model](database/README.md), when data is involved
6. [Decision register](DECISIONS.md)

Repository-wide agent behavior and source priority are defined in
[AGENTS.md](../AGENTS.md). Delivery orchestration lives under
[AI operation](../ai-operation/).

## Canonical sources

- Product documents describe intended behavior and scope.
- Active BDRs govern durable business and product choices.
- Active ADRs govern durable architecture and technology choices.
- Engineering standards govern recurring development practices.
- Logical database documentation describes data intent; versioned migrations
  govern the implemented physical schema.
- Epics, plans, and reports are delivery artifacts, not substitutes for durable
  decisions.
