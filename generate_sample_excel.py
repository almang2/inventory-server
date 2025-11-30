import pandas as pd

# Create sample data
data = {
    'Product Code': ['P00000LM000D', 'P00000WA000B', 'P00000IP000A'],
    'Quantity': [10, 5, 20],
    'Applied At': ['2023-12-01 10:00:00', '2023-12-01 11:00:00', '']
}

# Create DataFrame
df = pd.DataFrame(data)

# Save to Excel
df.to_excel('sample_retail.xlsx', index=False)

print("Sample Excel file 'sample_retail.xlsx' created successfully.")
